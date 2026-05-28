package fr.ailegalcase.document;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.ocr.OcrResult;
import fr.ailegalcase.ocr.OcrService;
import fr.ailegalcase.storage.StorageService;
import fr.ailegalcase.video.VideoExtractionException;
import fr.ailegalcase.video.VideoFrameExtractor;
import fr.ailegalcase.video.VideoFrameExtractorProperties;
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    /**
     * SF-230-01 : content types images supportés en upload natif (sans conversion PDF
     * intermédiaire). Textract accepte JPG/PNG/HEIC/WebP nativement.
     * Package-private pour testabilité (tests d'intégrité).
     */
    static final Set<String> IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/heic",
            "image/webp"
    );

    /** SF-230-01 : helper pour reconnaître un content type image supporté. */
    private static boolean isSupportedImageContentType(String contentType) {
        return contentType != null && IMAGE_CONTENT_TYPES.contains(contentType);
    }

    /** SF-231-01 : prompt système Vision pour analyse de scènes vidéo (5 frames). */
    private static final String VIDEO_VISION_SYSTEM_PROMPT = """
            Tu analyses 5 extraits temporels (frames) d'une même vidéo, présentés
            dans l'ordre chronologique (10%, 30%, 50%, 70%, 90% de la durée totale).
            Ces frames proviennent typiquement d'une caméra de surveillance, d'un
            téléphone, ou d'une captation visuelle utilisée comme pièce dans un
            dossier juridique.

            Produis une description structurée en français, factuelle et neutre,
            en 6 à 12 phrases, articulée autour de :
            - Cadre / contexte visuel : lieu présumé (intérieur / extérieur, type de
              local), conditions (jour / nuit, météo si visible), angle et qualité
              de la prise de vue.
            - Personnes visibles : nombre, posture, vêtements, distinction
              identifiable (acteur principal, témoin, tiers).
            - Objets et éléments matériels pertinents : véhicules, équipements,
              documents, supports affichés.
            - Évolution temporelle : ce qui change entre les frames (déplacement,
              interaction, événement déclencheur, conséquences).
            - Élements probants ou notables (bris, traces, présence d'arme, gestes
              violents, dégradations) si présents — décris-les sans interprétation
              juridique.

            Si la vidéo est vide / floue / corrompue : dis-le explicitement
            ("Frames illisibles, aucune scène identifiable").

            Pas de spéculation juridique. Pas de qualification pénale. Pas de
            citation de loi. Le but est qu'un avocat lisant uniquement ta
            description puisse comprendre ce que la vidéo montre sans la visionner.
            """;

    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final DocumentPieceRepository documentPieceRepository;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;
    private final OcrService ocrService;
    private final WorkspaceRepository workspaceRepository;
    private final OcrRunningFlagService ocrRunningFlagService;
    private final VideoFrameExtractor videoFrameExtractor;
    private final VideoFrameExtractorProperties videoProps;
    private final AnthropicService anthropicService;
    private final fr.ailegalcase.document.vision.VisionProperties visionProps;

    @Lazy @Autowired
    private ExtractionService self;

    public ExtractionService(DocumentRepository documentRepository,
                             DocumentExtractionRepository extractionRepository,
                             DocumentPieceRepository documentPieceRepository,
                             StorageService storageService,
                             ApplicationEventPublisher eventPublisher,
                             OcrService ocrService,
                             WorkspaceRepository workspaceRepository,
                             OcrRunningFlagService ocrRunningFlagService,
                             VideoFrameExtractor videoFrameExtractor,
                             VideoFrameExtractorProperties videoProps,
                             AnthropicService anthropicService,
                             fr.ailegalcase.document.vision.VisionProperties visionProps) {
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.documentPieceRepository = documentPieceRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
        this.ocrService = ocrService;
        this.workspaceRepository = workspaceRepository;
        this.ocrRunningFlagService = ocrRunningFlagService;
        this.videoFrameExtractor = videoFrameExtractor;
        this.videoProps = videoProps;
        this.anthropicService = anthropicService;
        this.visionProps = visionProps;
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
            String text = parseText(fileBytes, contentType, docRef);
            long duration = System.currentTimeMillis() - start;

            // SF-121-01 : détection du cas "texte vide" (PDF scanné sans couche texte).
            // SF-122-01 : sur PDF vide, tente un fallback OCR Textract avant de marquer FAILED.
            // SF-230-01 : pour les uploads natifs d'images (JPG/PNG/HEIC/WebP), parseText
            // retourne "" et on emprunte ici la même branche OCR (Textract accepte les
            // bytes image directement, pas de rasterisation PDF intermédiaire).
            boolean isImage = isSupportedImageContentType(contentType);
            boolean ocrEligible = ("application/pdf".equals(contentType) || isImage);
            if (text == null || text.isBlank()) {
                if (ocrEligible && docRef.isOcrEnabled()) {
                    boolean formsMode = docRef.isOcrFormsMode(); // SF-122-03
                    // SF-144-01 : flag intermédiaire lu par le polling frontend.
                    // REQUIRES_NEW → commité même si la transaction courante n'a pas flushé.
                    ocrRunningFlagService.markOcrRunning(extraction.getId(), true);
                    OcrResult ocr;
                    try {
                        // SF-230-01 : pour les images, route via extractFromImage
                        // (Textract direct sur bytes image, pas de rasterisation PDF).
                        if (isImage) {
                            ocr = extractFromImage(fileBytes, contentType, docRef);
                        } else {
                            UUID workspaceId = docRef.getCaseFile().getWorkspace().getId();
                            ocr = ocrService.tryOcr(fileBytes, workspaceId, formsMode);
                        }
                    } finally {
                        ocrRunningFlagService.markOcrRunning(extraction.getId(), false);
                    }
                    if (ocr.success()) {
                        extraction.setExtractedText(ocr.text());
                        int quotaPages = formsMode ? ocr.pageCount() * OcrService.FORMS_QUOTA_MULTIPLIER : ocr.pageCount();
                        String extractor = ocr.rasterized() ? "textract-rasterized" : "textract"; // SF-122-08
                        extraction.setExtractionMetadata(
                                "{\"extractor\":\"%s\",\"formsMode\":%s,\"charCount\":%d,\"pageCount\":%d,\"quotaPages\":%d,\"durationMs\":%d}"
                                        .formatted(extractor, formsMode, ocr.text().length(), ocr.pageCount(), quotaPages, duration));
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
        } catch (VideoExtractionException e) {
            // SF-231-01 : ffmpeg en échec ou timeout — motif explicite côté frontend.
            ExtractionFailureReason reason = e.getReason() == VideoExtractionException.Reason.VIDEO_EXTRACTION_TIMEOUT
                    ? ExtractionFailureReason.VIDEO_EXTRACTION_TIMEOUT
                    : ExtractionFailureReason.VIDEO_EXTRACTION_FAILED;
            log.warn("Extraction failed for document {} — video processing: {} ({})",
                    documentId, e.getMessage(), reason);
            extraction.setExtractionMetadata("{\"error\":\"%s\",\"reason\":\"%s\"}".formatted(
                    escapeJson(e.getMessage()), reason.name()));
            extraction.setExtractionStatus(ExtractionStatus.FAILED);
            extraction.setFailureReason(reason);
        } catch (VisionApiException e) {
            // SF-231-01 : appel Anthropic Vision en erreur — motif distinct de ffmpeg.
            log.warn("Extraction failed for document {} — Vision API: {}", documentId, e.getMessage());
            extraction.setExtractionMetadata("{\"error\":\"%s\",\"reason\":\"VISION_API_ERROR\"}".formatted(
                    escapeJson(e.getMessage())));
            extraction.setExtractionStatus(ExtractionStatus.FAILED);
            extraction.setFailureReason(ExtractionFailureReason.VISION_API_ERROR);
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
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            int updated = workspaceRepository.incrementOcrUsage(workspaceId, pageCount, today, monthStart);
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

    /**
     * Package-private pour testabilité directe (signature historique).
     * SF-231-01 : appelle {@link #parseText(byte[], String, Document)} avec docRef=null
     * — ne pourra pas écrire de DocumentPiece pour les vidéos (utile uniquement
     * pour les tests unitaires des branches non vidéo).
     */
    String parseText(byte[] fileBytes, String contentType) throws Exception {
        return parseText(fileBytes, contentType, null);
    }

    private String parseText(byte[] fileBytes, String contentType, Document docRef) throws Exception {
        return switch (contentType) {
            case "application/pdf" -> extractFromPdf(fileBytes);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                    extractFromDocx(fileBytes);
            case "application/msword" -> extractFromDoc(fileBytes);
            case "text/plain" -> new String(fileBytes, StandardCharsets.UTF_8);
            // SF-230-01 : pour les images natives (JPG/PNG/HEIC/WebP), aucune extraction
            // texte native — on retourne "" et le mécanisme empty-text → OCR Textract
            // (extractFromImage) prend le relais. Pas de PDFTextStripper sur le path image.
            case "image/jpeg", "image/png", "image/heic", "image/webp" -> "";
            // SF-231-01 : ingestion vidéo via ffmpeg + Vision multi-frames.
            case "video/mp4", "video/quicktime" -> extractFromVideo(fileBytes, contentType, docRef);
            default -> throw new IllegalArgumentException("Unsupported content type: " + contentType);
        };
    }

    /**
     * SF-231-01 : extrait 5 frames d'une vidéo via ffmpeg, soumet en batch à Claude Vision,
     * persiste la description retournée comme texte d'extraction et crée une
     * {@link DocumentPiece} de type {@link DocumentPieceType#PHOTO} avec
     * {@code visualDescription} renseigné (pattern miroir de F-148 SF-148-01).
     *
     * @param fileBytes bytes complets de la vidéo
     * @param contentType {@code video/mp4} ou {@code video/quicktime}
     * @param docRef document parent (peut être null en test pour la branche unitaire)
     * @return description Vision (utilisée comme {@code extractedText})
     */
    String extractFromVideo(byte[] fileBytes, String contentType, Document docRef) {
        Path tmpVideo = null;
        try {
            // 1. Écriture du fichier vidéo temporaire pour ffmpeg.
            String suffix = "video/quicktime".equals(contentType) ? ".mov" : ".mp4";
            tmpVideo = Files.createTempFile("legalcase-video-" + UUID.randomUUID() + "-", suffix);
            Files.write(tmpVideo, fileBytes);

            // 2. Extraction de 5 frames via ffmpeg.
            List<byte[]> frames = videoFrameExtractor.extract5Frames(tmpVideo.toFile());
            log.info("Extracted {} frames from video {}", frames.size(),
                    docRef != null ? docRef.getId() : "(test)");

            // 3. Appel Vision multi-frames.
            String userText = buildVideoUserText(contentType, frames.size());
            String visionModel = visionProps.getModel();
            AnthropicResult result;
            try {
                UUID caseFileId = (docRef != null && docRef.getCaseFile() != null)
                        ? docRef.getCaseFile().getId() : null;
                AiCallContext ctx = caseFileId != null
                        ? AiCallContext.systemLevel(JobType.SYSTEM_VISION_ENRICHMENT, caseFileId)
                        : AiCallContext.systemLevel(JobType.SYSTEM_VISION_ENRICHMENT);
                result = anthropicService.analyzeWithImages(ctx, visionModel, VIDEO_VISION_SYSTEM_PROMPT,
                        frames, "image/png", userText, 2000);
            } catch (Exception e) {
                log.error("Vision API failed for video document {} — {}",
                        docRef != null ? docRef.getId() : "(test)", e.getMessage());
                throw new VisionApiException("Vision API error during video analysis: " + e.getMessage(), e);
            }

            String description = result.content() == null ? null : result.content().trim();
            if (description == null || description.isBlank()) {
                throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                        "Vision returned empty description for video");
            }

            // 4. Persistance d'une DocumentPiece PHOTO (visualDescription) — pattern F-148.
            if (docRef != null) {
                persistVideoPiece(docRef, description, visionModel);
            }
            return description;
        } catch (IOException e) {
            throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                    "Failed to write video tmp file: " + e.getMessage(), e);
        } finally {
            if (tmpVideo != null) {
                try {
                    Files.deleteIfExists(tmpVideo);
                } catch (IOException e) {
                    log.warn("Failed to delete tmp video file {}: {}", tmpVideo, e.getMessage());
                }
            }
        }
    }

    private void persistVideoPiece(Document docRef, String description, String visionModel) {
        DocumentPiece piece = new DocumentPiece();
        piece.setDocument(docRef);
        piece.setType(DocumentPieceType.PHOTO);
        piece.setLabel("Vidéo");
        piece.setPageStart(1);
        piece.setPageEnd(1);
        piece.setOrderIndex(0);
        piece.setVisualDescription(description);
        piece.setVisionEnrichedAt(Instant.now());
        piece.setVisionModel(visionModel);
        piece.setVisionStatus(VisionStatus.DONE);
        documentPieceRepository.save(piece);
        log.info("Persisted video DocumentPiece (PHOTO) for document {} — {} chars description",
                docRef.getId(), description.length());
    }

    private String buildVideoUserText(String contentType, int frameCount) {
        return "Vidéo à analyser :\n"
                + "- Format source : " + contentType + "\n"
                + "- Frames fournies : " + frameCount + " (ordre chronologique 10/30/50/70/90% de la durée)\n"
                + "- Audio : non fourni (V1 silencieuse — la transcription audio est V2).\n";
    }

    /** Wrapper interne pour différencier l'erreur Vision de l'erreur ffmpeg dans le catch. */
    static class VisionApiException extends RuntimeException {
        VisionApiException(String message, Throwable cause) { super(message, cause); }
    }

    private String extractFromPdf(byte[] fileBytes) throws Exception {
        try (PDDocument doc = Loader.loadPDF(fileBytes)) {
            // SF-145-07 : extraction page par page avec marqueurs explicites
            // "=== PAGE N ===" pour que DocumentPieceDetectionService puisse
            // déterminer précisément pageStart/pageEnd de chaque pièce.
            // Aussi utile pour le pipeline IA principal (structure plus claire).
            // On n'ajoute un marqueur QUE si la page contient du texte non-blank —
            // sinon un PDF scanné sans couche texte produirait faussement un texte
            // non-vide (juste les marqueurs), ce qui masquerait le déclenchement
            // du fallback OCR Textract.
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = doc.getNumberOfPages();
            StringBuilder full = new StringBuilder();
            for (int pageIdx = 1; pageIdx <= pageCount; pageIdx++) {
                stripper.setStartPage(pageIdx);
                stripper.setEndPage(pageIdx);
                String pageText = stripper.getText(doc);
                if (pageText == null || pageText.isBlank()) continue;
                if (full.length() > 0) full.append("\n\n");
                full.append("=== PAGE ").append(pageIdx).append(" ===\n");
                full.append(pageText);
            }
            return full.toString();
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

    /**
     * SF-230-01 : extraction d'une image native (JPG/PNG/HEIC/WebP) via OCR Textract direct.
     *
     * <p>Contrairement au PDF, on n'enrobe PAS l'image dans un PDF avant d'appeler
     * Textract — Textract accepte directement les bytes image. Économie : pas
     * d'overhead PDF, pas de dépendance PDFBox sur le path image.
     *
     * <p>Aucun PDFTextStripper n'est invoqué : une image n'a pas de couche texte
     * native, l'extraction passe entièrement par Textract.
     *
     * <p>Si {@code docRef.isOcrEnabled()} est false, l'extraction est skippée et
     * marquée FAILED EMPTY_TEXT par le caller (cf. branche dans {@link #extract}).
     *
     * <p>Quota OCR : 1 image = 1 page comptabilisée (pageCount renvoyé par
     * {@code OcrService.tryOcr}, qui plafonne à 1 sur les images via le fallback
     * {@code countPdfPages}).
     *
     * <p>Package-private pour permettre les tests d'intégrité de routing.
     *
     * @param fileBytes   bytes de l'image originale
     * @param contentType {@code image/jpeg}, {@code image/png}, {@code image/heic}, {@code image/webp}
     * @param docRef      document de référence (porte {@code ocrEnabled}, {@code ocrFormsMode}, workspace)
     * @return résultat OCR (success+text+pageCount ou failure+motif)
     */
    OcrResult extractFromImage(byte[] fileBytes, String contentType, Document docRef) {
        if (!isSupportedImageContentType(contentType)) {
            throw new IllegalArgumentException("extractFromImage called with non-image contentType: " + contentType);
        }
        UUID workspaceId = docRef.getCaseFile().getWorkspace().getId();
        boolean formsMode = docRef.isOcrFormsMode();
        return ocrService.tryOcr(fileBytes, workspaceId, formsMode);
    }
}
