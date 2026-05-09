package fr.ailegalcase.document.vision;

import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentPiece;
import fr.ailegalcase.document.DocumentPieceRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.document.VisionStatus;
import fr.ailegalcase.storage.StorageService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SF-148-01 : enrichit chaque pièce éligible avec une description visuelle
 * produite par Claude Vision. Écoute {@link PiecesPersistedEvent} en
 * AFTER_COMMIT et dispatche asynchronement.
 *
 * <p>Stratégie hybride (cf. {@link VisionProperties}) :
 * <ul>
 *   <li>Signal 1 : OCR < min-chars/page → vision forcée</li>
 *   <li>Signal 2 : type en whitelist domaine → vision déclenchée</li>
 *   <li>Blacklist : type skippé sauf signal 1</li>
 * </ul>
 *
 * <p>Fail-open : toute exception est logguée en warning et la pièce reste
 * utilisable sans enrichissement.
 */
@Service
public class VisionEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(VisionEnrichmentService.class);

    private static final String VISION_SYSTEM_PROMPT = """
            Tu décris précisément le contenu visuel de pièces juridiques scannées.

            Sortie : description courte et factuelle (4 à 8 phrases) mettant en
            évidence les éléments que l'OCR perd :
            - qui parle / à qui (SMS, messagerie : identifier les interlocuteurs par position, couleur, pseudo)
            - disposition spatiale et structure visuelle (tableau, colonne, schéma)
            - éléments non textuels : photo, logo, tampon, cachet, signature
            - annotations manuscrites, corrections, ratures
            - qualité visuelle (scan dégradé, flou, rotation anormale)

            Ne reformule PAS le texte déjà parfaitement lisible. Concentre-toi sur
            la valeur ajoutée visuelle. Si la pièce est purement textuelle et lisible,
            réponds "Contenu purement textuel, aucune information visuelle additionnelle".

            Français neutre, ton factuel, pas de spéculation juridique.
            """;

    private final DocumentRepository documentRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final DocumentPieceRepository pieceRepository;
    private final AnthropicService anthropicService;
    private final StorageService storageService;
    private final VisionProperties props;
    private final TaskExecutor taskExecutor;

    @Lazy @Autowired
    private VisionEnrichmentService self;

    public VisionEnrichmentService(DocumentRepository documentRepository,
                                   DocumentExtractionRepository extractionRepository,
                                   DocumentPieceRepository pieceRepository,
                                   AnthropicService anthropicService,
                                   StorageService storageService,
                                   VisionProperties props,
                                   @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.documentRepository = documentRepository;
        this.extractionRepository = extractionRepository;
        this.pieceRepository = pieceRepository;
        this.anthropicService = anthropicService;
        this.storageService = storageService;
        this.props = props;
        this.taskExecutor = taskExecutor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPiecesPersisted(PiecesPersistedEvent event) {
        if (!props.isEnabled()) {
            log.debug("Vision disabled — skipping enrichment for document {}", event.documentId());
            return;
        }
        taskExecutor.execute(() -> enrichDocumentSafely(event.documentId(), event.legalDomain()));
    }

    private void enrichDocumentSafely(UUID documentId, String legalDomain) {
        try {
            enrichDocument(documentId, legalDomain);
        } catch (Exception e) {
            log.warn("Vision enrichment crashed for document {} — {}", documentId, e.getMessage());
        }
    }

    /**
     * Deux phases :
     * 1. <b>Phase PENDING</b> (courte, dédiée transaction) — marque toutes les pièces
     *    éligibles comme {@link VisionStatus#PENDING}, commit immédiat. Le frontend
     *    affiche le spinner.
     * 2. <b>Phase enrichissement</b> — pour chaque pièce PENDING, appel Anthropic
     *    puis update status {@code DONE}/{@code FAILED} dans une transaction dédiée
     *    (self-proxy) afin que le frontend voie chaque pièce basculer
     *    individuellement.
     */
    public void enrichDocument(UUID documentId, String legalDomain) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) {
            log.warn("Document {} not found — skipping vision enrichment", documentId);
            return;
        }
        // SF-230-01 : accepte aussi les images natives JPG/PNG/WebP (HEIC non supporté
        // par Anthropic Vision — skipped). Le bytes téléchargé sera utilisé directement
        // sans rasterisation PDF (cf. enrichPendingPiece + renderPiecePages).
        boolean isPdf = isPdf(document);
        boolean isVisionImage = isVisionCompatibleImage(document);
        if (!isPdf && !isVisionImage) {
            log.info("Document {} content type ({}) not supported by Vision — skipping",
                    documentId, document.getContentType());
            return;
        }

        Map<Integer, String> textByPage = self.loadTextByPage(documentId);

        // Phase 1 — marque PENDING (commit immédiat).
        List<UUID> pendingPieceIds = self.markEligibleAsPending(documentId, legalDomain, textByPage);
        if (pendingPieceIds.isEmpty()) return;

        byte[] sourceBytes = storageService.download(document.getStorageKey());
        String contentType = document.getContentType();

        // Phase 2 — enrichissement pièce par pièce.
        for (UUID pieceId : pendingPieceIds) {
            self.enrichPendingPiece(pieceId, sourceBytes, contentType);
        }
    }

    /**
     * Phase 1 : pour chaque pièce éligible et non encore DONE, marque PENDING.
     * Retourne les IDs à enrichir dans la phase 2.
     */
    @Transactional
    public List<UUID> markEligibleAsPending(UUID documentId, String legalDomain,
                                            Map<Integer, String> textByPage) {
        List<DocumentPiece> pieces = pieceRepository.findByDocument_IdOrderByOrderIndexAsc(documentId);
        List<UUID> pendingIds = new ArrayList<>();
        for (DocumentPiece piece : pieces) {
            if (piece.getVisionStatus() == VisionStatus.DONE) continue; // idempotence
            if (!shouldEnrichPiece(piece, legalDomain, textByPage)) continue;
            piece.setVisionStatus(VisionStatus.PENDING);
            pieceRepository.save(piece);
            pendingIds.add(piece.getId());
        }
        log.info("Marked {} piece(s) PENDING for vision enrichment on document {}",
                pendingIds.size(), documentId);
        return pendingIds;
    }

    /**
     * Phase 2 : appelle Anthropic + update status. Transaction dédiée pour que
     * chaque pièce bascule individuellement visible côté frontend.
     *
     * @param sourceBytes bytes du document source (PDF ou image originale)
     * @param contentType content type du document — détermine si on rasterise
     *                    (PDF) ou si on envoie les bytes directs (image SF-230-01)
     */
    @Transactional
    public void enrichPendingPiece(UUID pieceId, byte[] sourceBytes, String contentType) {
        DocumentPiece piece = pieceRepository.findById(pieceId).orElse(null);
        if (piece == null || piece.getVisionStatus() != VisionStatus.PENDING) return;
        enrichPieceSafely(piece, sourceBytes, contentType);
    }

    /** Surcharge rétrocompat — défaut PDF (cf. SF-148-01 avant SF-230-01). */
    @Transactional
    public void enrichPendingPiece(UUID pieceId, byte[] pdfBytes) {
        enrichPendingPiece(pieceId, pdfBytes, "application/pdf");
    }

    /**
     * Charge le texte par page en transaction dédiée (pour éviter de garder la
     * connection hibernate ouverte pendant les appels Anthropic de phase 2).
     */
    @Transactional(readOnly = true)
    public Map<Integer, String> loadTextByPage(UUID documentId) {
        Document document = documentRepository.findById(documentId).orElse(null);
        if (document == null) return Map.of();
        return extractTextByPage(document);
    }

    /**
     * Décide si la pièce doit recevoir une description visuelle, selon la stratégie
     * hybride signaux + config. Exposé package-private pour tests.
     */
    boolean shouldEnrichPiece(DocumentPiece piece, String legalDomain, Map<Integer, String> textByPage) {
        if (!props.isEnabled()) return false;

        // Signal 1 — prioritaire : au moins une page avec OCR très pauvre
        for (int p = piece.getPageStart(); p <= piece.getPageEnd(); p++) {
            String pageText = textByPage.getOrDefault(p, "");
            if (pageText.trim().length() < props.getMinOcrCharsPerPage()) {
                return true;
            }
        }

        String typeName = piece.getType().name();

        // Blacklist (appliquée seulement quand signal 1 ne l'a pas forcé à true)
        Set<String> blacklist = skipTypesFor(legalDomain);
        if (blacklist.contains(typeName)) return false;

        // Signal 2 — whitelist
        Set<String> whitelist = triggerTypesFor(legalDomain);
        return whitelist.contains(typeName);
    }

    private Set<String> triggerTypesFor(String legalDomain) {
        if (legalDomain == null) return Set.of();
        return props.getTriggerTypesByDomain().getOrDefault(legalDomain, Set.of());
    }

    private Set<String> skipTypesFor(String legalDomain) {
        if (legalDomain == null) return Set.of();
        return props.getSkipTypesByDomain().getOrDefault(legalDomain, Set.of());
    }

    private void enrichPieceSafely(DocumentPiece piece, byte[] sourceBytes, String contentType) {
        try {
            // SF-230-01 : si le document source est déjà une image native compatible
            // Vision (JPG/PNG/WebP), envoyer les bytes directs sans rasterisation.
            // Sinon (PDF), rasteriser les pages couvertes par la pièce.
            List<byte[]> pageImages;
            String mediaType;
            if (isVisionCompatibleImageContentType(contentType)) {
                pageImages = List.of(sourceBytes);
                mediaType = contentType;
            } else {
                pageImages = renderPagesAsPng(sourceBytes, piece.getPageStart(), piece.getPageEnd());
                mediaType = "image/png";
            }
            if (pageImages.isEmpty()) {
                piece.setVisionStatus(VisionStatus.FAILED);
                pieceRepository.save(piece);
                return;
            }

            String userText = buildUserText(piece);
            AnthropicResult result = anthropicService.analyzeWithImages(
                    props.getModel(),
                    VISION_SYSTEM_PROMPT,
                    pageImages,
                    mediaType,
                    userText,
                    props.getMaxTokens()
            );

            String description = result.content() == null ? null : result.content().trim();
            if (description == null || description.isBlank()) {
                log.warn("Vision returned empty description for piece {} — marking FAILED", piece.getId());
                piece.setVisionStatus(VisionStatus.FAILED);
                pieceRepository.save(piece);
                return;
            }

            piece.setVisualDescription(description);
            piece.setVisionEnrichedAt(Instant.now());
            piece.setVisionModel(props.getModel());
            piece.setVisionStatus(VisionStatus.DONE);
            pieceRepository.save(piece);
            log.info("Vision enriched piece {} (type={}, pages={}-{}, {} chars)",
                    piece.getId(), piece.getType(), piece.getPageStart(), piece.getPageEnd(),
                    description.length());
        } catch (Exception e) {
            log.warn("Vision enrichment failed for piece {} — {}", piece.getId(), e.getMessage());
            try {
                piece.setVisionStatus(VisionStatus.FAILED);
                pieceRepository.save(piece);
            } catch (Exception persistFail) {
                log.warn("Could not persist FAILED status for piece {}", piece.getId());
            }
        }
    }

    private String buildUserText(DocumentPiece piece) {
        StringBuilder sb = new StringBuilder();
        sb.append("Pièce à décrire visuellement :\n");
        sb.append("- Type : ").append(piece.getType().name()).append("\n");
        if (piece.getLabel() != null && !piece.getLabel().isBlank()) {
            sb.append("- Label : ").append(piece.getLabel()).append("\n");
        }
        int pageCount = piece.getPageEnd() - piece.getPageStart() + 1;
        sb.append("- Pages fournies : ").append(pageCount).append(" (ordre préservé)\n");
        return sb.toString();
    }

    private List<byte[]> renderPagesAsPng(byte[] pdfBytes, int pageStart, int pageEnd) {
        List<byte[]> out = new ArrayList<>();
        try (PDDocument pdf = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            int total = pdf.getNumberOfPages();
            int from = Math.max(1, pageStart);
            int to = Math.min(total, pageEnd);
            for (int page = from; page <= to; page++) {
                try {
                    BufferedImage image = renderer.renderImageWithDPI(page - 1, props.getRenderDpi(), ImageType.RGB);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "PNG", baos);
                    out.add(baos.toByteArray());
                } catch (Exception e) {
                    log.warn("Failed to render page {} to PNG — {}", page, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load PDF for rasterization — {}", e.getMessage());
        }
        return out;
    }

    /**
     * Extrait le texte par page depuis le texte extrait déjà persisté (qui
     * contient les marqueurs "=== PAGE N ===" depuis SF-145-07).
     */
    Map<Integer, String> extractTextByPage(Document document) {
        DocumentExtraction extraction = extractionRepository.findByDocumentId(document.getId()).orElse(null);
        String fullText = extraction != null ? extraction.getExtractedText() : null;
        if (fullText == null || fullText.isBlank()) return Map.of();

        Map<Integer, String> result = new java.util.HashMap<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?m)^=== PAGE (\\d+) ===$").matcher(fullText);
        List<int[]> marks = new ArrayList<>();
        while (m.find()) {
            marks.add(new int[]{Integer.parseInt(m.group(1)), m.start()});
        }
        if (marks.isEmpty()) return Map.of(); // pas de marqueurs : on ne peut pas isoler

        for (int i = 0; i < marks.size(); i++) {
            int pageNum = marks.get(i)[0];
            int start = marks.get(i)[1];
            int end = i + 1 < marks.size() ? marks.get(i + 1)[1] : fullText.length();
            String section = fullText.substring(start, end);
            int nl = section.indexOf('\n');
            String body = nl < 0 ? "" : section.substring(nl + 1);
            result.put(pageNum, body);
        }
        return result;
    }

    private boolean isPdf(Document document) {
        String ct = document.getContentType();
        return ct != null && ct.toLowerCase().contains("pdf");
    }

    /**
     * SF-230-01 : true si le document est une image native directement consommable
     * par Anthropic Vision (JPG/PNG/WebP). HEIC est exclu : Anthropic Vision ne
     * le supporte pas et la conversion HEIC→PNG nécessite une lib tierce.
     * Pour HEIC, l'OCR Textract reste utilisable mais l'enrichissement Vision est skippé.
     */
    private boolean isVisionCompatibleImage(Document document) {
        return isVisionCompatibleImageContentType(document.getContentType());
    }

    /** Voir {@link #isVisionCompatibleImage(Document)}. */
    private static boolean isVisionCompatibleImageContentType(String contentType) {
        return "image/jpeg".equals(contentType)
                || "image/png".equals(contentType)
                || "image/webp".equals(contentType);
    }
}
