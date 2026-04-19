package fr.ailegalcase.document;

import fr.ailegalcase.ocr.OcrResult;
import fr.ailegalcase.ocr.OcrService;
import fr.ailegalcase.storage.StorageService;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;
    private final OcrService ocrService;
    private final WorkspaceRepository workspaceRepository;

    @Lazy @Autowired
    private ExtractionService self;

    public ExtractionService(DocumentRepository documentRepository,
                             DocumentExtractionRepository extractionRepository,
                             StorageService storageService,
                             ApplicationEventPublisher eventPublisher,
                             OcrService ocrService,
                             WorkspaceRepository workspaceRepository) {
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
        this.ocrService = ocrService;
        this.workspaceRepository = workspaceRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        self.extract(event.documentId(), event.storageKey(), event.contentType());
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

            // SF-121-01 : détection du cas "texte vide" (PDF scanné sans couche texte).
            // SF-122-01 : sur PDF vide, tente un fallback OCR Textract avant de marquer FAILED.
            if (text == null || text.isBlank()) {
                if ("application/pdf".equals(contentType) && docRef.isOcrEnabled()) {
                    UUID workspaceId = docRef.getCaseFile().getWorkspace().getId();
                    boolean formsMode = docRef.isOcrFormsMode(); // SF-122-03
                    OcrResult ocr = ocrService.tryOcr(fileBytes, workspaceId, formsMode);
                    if (ocr.success()) {
                        extraction.setExtractedText(ocr.text());
                        int quotaPages = formsMode ? ocr.pageCount() * OcrService.FORMS_QUOTA_MULTIPLIER : ocr.pageCount();
                        extraction.setExtractionMetadata(
                                "{\"extractor\":\"textract\",\"formsMode\":%s,\"charCount\":%d,\"pageCount\":%d,\"quotaPages\":%d,\"durationMs\":%d}"
                                        .formatted(formsMode, ocr.text().length(), ocr.pageCount(), quotaPages, duration));
                        extraction.setExtractionStatus(ExtractionStatus.DONE);
                        extraction.setFailureReason(null);
                        incrementOcrUsage(docRef, quotaPages);
                        log.info("Extraction done via OCR for document {} — {} chars from {} page(s) (formsMode={}, quotaPages={})",
                                documentId, ocr.text().length(), ocr.pageCount(), formsMode, quotaPages);
                    } else {
                        log.warn("Extraction {} for document {} produced empty text — OCR also failed ({})",
                                extraction.getId(), documentId, ocr.failureMotif());
                        extraction.setExtractedText(null);
                        extraction.setExtractionMetadata(
                                "{\"extractor\":\"internal+textract\",\"charCount\":0,\"durationMs\":%d,\"reason\":\"%s\"}"
                                        .formatted(duration, ocr.failureMotif().name()));
                        extraction.setExtractionStatus(ExtractionStatus.FAILED);
                        extraction.setFailureReason(ocr.failureMotif());
                    }
                } else {
                    log.warn("Extraction {} for document {} produced empty text — marking FAILED (EMPTY_TEXT)",
                            extraction.getId(), documentId);
                    extraction.setExtractedText(null);
                    extraction.setExtractionMetadata(
                            "{\"extractor\":\"internal\",\"charCount\":0,\"durationMs\":%d,\"reason\":\"EMPTY_TEXT\"}".formatted(duration));
                    extraction.setExtractionStatus(ExtractionStatus.FAILED);
                    extraction.setFailureReason(ExtractionFailureReason.EMPTY_TEXT);
                }
            } else {
                extraction.setExtractedText(text);
                extraction.setExtractionMetadata(
                        "{\"extractor\":\"internal\",\"charCount\":%d,\"durationMs\":%d}".formatted(text.length(), duration));
                extraction.setExtractionStatus(ExtractionStatus.DONE);
                extraction.setFailureReason(null);
                log.info("Extraction done for document {} — {} chars in {}ms", documentId, text.length(), duration);
            }
        } catch (IllegalArgumentException e) {
            // parseText lève IllegalArgumentException pour les contentTypes non supportés.
            log.warn("Extraction failed for document {} — unsupported format: {}", documentId, e.getMessage());
            extraction.setExtractionMetadata("{\"error\":\"%s\",\"reason\":\"UNSUPPORTED_FORMAT\"}".formatted(
                    escapeJson(e.getMessage())));
            extraction.setExtractionStatus(ExtractionStatus.FAILED);
            extraction.setFailureReason(ExtractionFailureReason.UNSUPPORTED_FORMAT);
        } catch (Exception e) {
            // Distinction CORRUPTED (parsing PDF/DOCX) vs EXTRACTION_EXCEPTION (autres).
            ExtractionFailureReason reason = isParsingException(e)
                    ? ExtractionFailureReason.CORRUPTED
                    : ExtractionFailureReason.EXTRACTION_EXCEPTION;
            log.error("Extraction failed for document {} — reason={}, error={}",
                    documentId, reason, e.getMessage(), e);
            extraction.setExtractionMetadata("{\"error\":\"%s\",\"reason\":\"%s\"}".formatted(
                    escapeJson(e.getMessage()), reason.name()));
            extraction.setExtractionStatus(ExtractionStatus.FAILED);
            extraction.setFailureReason(reason);
        }

        extractionRepository.save(extraction);

        // Publie l'événement uniquement si DONE — empêche ChunkingService d'être déclenché sur FAILED.
        if (extraction.getExtractionStatus() == ExtractionStatus.DONE) {
            eventPublisher.publishEvent(new ExtractionDoneEvent(extraction.getId(), extraction.getExtractedText()));
        } else if (extraction.getExtractionStatus() == ExtractionStatus.FAILED) {
            // SF-121-02 : déclenche notification in-app + email au créateur du dossier
            String filename = docRef.getOriginalFilename();
            eventPublisher.publishEvent(new ExtractionFailedEvent(
                    extraction.getId(), documentId, filename, extraction.getFailureReason()));
        }
    }

    /**
     * SF-122-01 : incrémente atomiquement les compteurs OCR du workspace du document.
     * Fail-open : une erreur ici ne fait pas retomber l'extraction elle-même en FAILED
     * (le texte OCR a bien été persisté, la pipeline IA peut repartir normalement).
     */
    private void incrementOcrUsage(Document docRef, int pageCount) {
        if (pageCount <= 0) return;
        try {
            UUID workspaceId = docRef.getCaseFile().getWorkspace().getId();
            int updated = workspaceRepository.incrementOcrUsage(workspaceId, pageCount, LocalDate.now());
            if (updated == 0) {
                log.warn("OCR usage increment found no workspace row for documentId={}, pageCount={}",
                        docRef.getId(), pageCount);
            }
        } catch (Exception e) {
            log.error("OCR usage increment failed for documentId={} — fail-open (pipeline continue)",
                    docRef.getId(), e);
        }
    }

    /**
     * Distingue une erreur de parsing de fichier (CORRUPTED) d'une autre exception.
     * Regarde la classe de l'exception ET le sommet de la stack trace car PDFBox
     * propage fréquemment un {@code java.io.IOException} générique dont le top frame
     * est {@code org.apache.pdfbox.pdfparser.*}.
     */
    private static boolean isParsingException(Exception e) {
        if (e == null) return false;
        String className = e.getClass().getName();
        if (className.startsWith("org.apache.pdfbox.") || className.startsWith("org.apache.poi.")) {
            return true;
        }
        StackTraceElement[] stack = e.getStackTrace();
        if (stack != null && stack.length > 0) {
            String topClass = stack[0].getClassName();
            return topClass.startsWith("org.apache.pdfbox.") || topClass.startsWith("org.apache.poi.");
        }
        return false;
    }

    /** Echappe les doubles-quotes pour insertion dans JSON string. */
    private static String escapeJson(String raw) {
        return raw != null ? raw.replace("\"", "'") : "unknown";
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
