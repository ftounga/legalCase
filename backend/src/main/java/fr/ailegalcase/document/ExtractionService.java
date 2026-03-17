package fr.ailegalcase.document;

import fr.ailegalcase.storage.StorageService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;

    public ExtractionService(DocumentRepository documentRepository,
                             DocumentExtractionRepository extractionRepository,
                             StorageService storageService,
                             ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        extract(event.documentId(), event.storageKey(), event.contentType());
    }

    @Transactional
    public void extract(UUID documentId, String storageKey, String contentType) {
        Document docRef = documentRepository.getReferenceById(documentId);

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(docRef);
        extraction.setExtractionStatus(ExtractionStatus.PENDING);
        extraction = extractionRepository.save(extraction);

        extraction.setExtractionStatus(ExtractionStatus.PROCESSING);
        extractionRepository.save(extraction);

        try {
            byte[] fileBytes = storageService.download(storageKey);
            long start = System.currentTimeMillis();
            String text = parseText(fileBytes, contentType);
            long duration = System.currentTimeMillis() - start;

            extraction.setExtractedText(text);
            extraction.setExtractionMetadata(
                    "{\"extractor\":\"internal\",\"charCount\":%d,\"durationMs\":%d}".formatted(text.length(), duration));
            extraction.setExtractionStatus(ExtractionStatus.DONE);
            log.info("Extraction done for document {} — {} chars in {}ms", documentId, text.length(), duration);
        } catch (Exception e) {
            log.error("Extraction failed for document {}", documentId, e);
            extraction.setExtractionMetadata("{\"error\":\"%s\"}".formatted(
                    e.getMessage() != null ? e.getMessage().replace("\"", "'") : "unknown"));
            extraction.setExtractionStatus(ExtractionStatus.FAILED);
        }

        extractionRepository.save(extraction);

        if (extraction.getExtractionStatus() == ExtractionStatus.DONE) {
            eventPublisher.publishEvent(new ExtractionDoneEvent(extraction.getId(), extraction.getExtractedText()));
        }
    }

    private String parseText(byte[] fileBytes, String contentType) throws Exception {
        return switch (contentType) {
            case "application/pdf" -> extractFromPdf(fileBytes);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    extractFromDocx(fileBytes);
            case "application/msword" -> extractFromDoc(fileBytes);
            case "text/plain" -> new String(fileBytes, StandardCharsets.UTF_8);
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }

    private String extractFromPdf(byte[] fileBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(fileBytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractFromDocx(byte[] fileBytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(fileBytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String extractFromDoc(byte[] fileBytes) throws Exception {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(fileBytes));
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
