package fr.ailegalcase.ocr;

import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.document.ExtractionFailureReason;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeDocumentRequest;
import software.amazon.awssdk.services.textract.model.AnalyzeDocumentResponse;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.Document;
import software.amazon.awssdk.services.textract.model.FeatureType;
import software.amazon.awssdk.services.textract.model.UnsupportedDocumentException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Fallback OCR via AWS Textract — SF-122-01.
 *
 * Appelé par {@link fr.ailegalcase.document.ExtractionService} quand
 * {@code PDFTextStripper} renvoie un texte vide et que le fichier est un PDF.
 *
 * Mode synchrone API (AnalyzeDocument / FeatureType.TABLES). Contraintes AWS :
 * ≤ 5 Mo et ≤ 11 pages (limites de l'API sync). Documents plus volumineux
 * renvoient {@link ExtractionFailureReason#OCR_UNSUPPORTED_SIZE} sans appel AWS
 * (mode async via {@code StartDocumentAnalysis} = itération future, hors scope V1).
 *
 * Toggle via {@code aws.textract.enabled} — désactivé en dev pour éviter tout
 * coût AWS.
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final TextractClient textractClient;
    private final OcrProperties properties;
    private final PlanLimitService planLimitService;
    /**
     * SF-122-09 : limite les calls Textract simultanés (acquire/release autour
     * de chaque analyzeDocument). Empêche les bursts parallèles (9 extractions
     * qui démarrent en même temps) de saturer AWS et de déclencher des
     * ProvisionedThroughputExceededException non rattrapables.
     */
    private final Semaphore textractSemaphore;

    public OcrService(Optional<TextractClient> textractClient,
                      OcrProperties properties,
                      PlanLimitService planLimitService) {
        this.textractClient = textractClient.orElse(null);
        this.properties = properties;
        this.planLimitService = planLimitService;
        this.textractSemaphore = new Semaphore(properties.maxConcurrentCalls());
    }

    /** SF-122-03 : coefficient de consommation quota du mode FORMS (Textract 4,3× plus cher, marge 30 %). */
    public static final int FORMS_QUOTA_MULTIPLIER = 3;

    /**
     * Tente une extraction OCR du document. Idempotent (aucun état interne).
     * Ne met pas à jour le compteur {@code ocr_pages_used} du workspace —
     * ExtractionService s'en charge via {@code WorkspaceRepository.incrementOcrUsage}.
     *
     * Ordre des checks :
     * 1. toggle {@code aws.textract.enabled}
     * 2. taille fichier ≤ 5 Mo (AWS sync)
     * 3. nombre de pages PDF ≤ 11 (AWS sync)
     * 4. SF-122-02 : quota mensuel + hard cap journalier du workspace
     *    (pages effectives = pdfPages × 3 si formsMode=true)
     * 5. appel Textract (FeatureType.TABLES + FORMS si formsMode, TABLES seul sinon)
     *
     * @param fileBytes   contenu binaire du fichier
     * @param workspaceId workspace du document (pour le gate quota)
     * @param formsMode   SF-122-03 — true pour analyse approfondie (CERFA / formulaires admin),
     *                    active FeatureType.FORMS (× 3 quota)
     * @return {@link OcrResult} success avec texte + pageCount, ou failure avec motif
     */
    public OcrResult tryOcr(byte[] fileBytes, UUID workspaceId, boolean formsMode) {
        if (!properties.enabled() || textractClient == null) {
            log.debug("OCR skipped — textract.enabled=false");
            return OcrResult.failure(ExtractionFailureReason.EMPTY_TEXT);
        }

        int sizeMb = fileBytes.length / (1024 * 1024);
        if (sizeMb >= properties.maxSizeMb()) {
            log.info("OCR skipped — document too large ({} MB > {} MB max)", sizeMb, properties.maxSizeMb());
            return OcrResult.failure(ExtractionFailureReason.OCR_UNSUPPORTED_SIZE);
        }

        int pdfPages = countPdfPages(fileBytes);
        if (pdfPages > properties.maxPages()) {
            log.info("OCR skipped — document has too many pages ({} > {} max)", pdfPages, properties.maxPages());
            return OcrResult.failure(ExtractionFailureReason.OCR_UNSUPPORTED_SIZE);
        }

        // SF-122-02 + SF-122-03 : gate quota avec multiplier FORMS si actif.
        int quotaPages = formsMode ? pdfPages * FORMS_QUOTA_MULTIPLIER : pdfPages;
        if (workspaceId != null && planLimitService.isOcrQuotaExceeded(workspaceId, quotaPages)) {
            log.info("OCR blocked — quota exceeded for workspace {} (formsMode={}, {} pages effective)",
                    workspaceId, formsMode, quotaPages);
            return OcrResult.failure(ExtractionFailureReason.OCR_QUOTA_EXCEEDED);
        }

        try {
            return callTextractDirect(fileBytes, formsMode);
        } catch (UnsupportedDocumentException e) {
            // SF-122-08 : Textract refuse le format PDF (CCITT Group 4, rotation metadata
            // exotique, encryption partielle, PDF linéarisé non-standard). On retente en
            // rasterisant chaque page localement en PNG via PDFBox, format universel
            // toujours accepté par Textract.
            log.info("Textract refused PDF format ({}), falling back to page-by-page rasterization",
                    e.getMessage());
            return callTextractRasterized(fileBytes, formsMode, pdfPages);
        } catch (Exception e) {
            log.error("OCR failed via Textract — class={}, message={}", e.getClass().getSimpleName(), e.getMessage());
            return OcrResult.failure(ExtractionFailureReason.OCR_FAILED);
        }
    }

    /** Call Textract direct sur le PDF entier (voie rapide, 1 call). */
    private OcrResult callTextractDirect(byte[] fileBytes, boolean formsMode) {
        AnalyzeDocumentRequest.Builder requestBuilder = AnalyzeDocumentRequest.builder()
                .document(Document.builder()
                        .bytes(SdkBytes.fromByteArray(fileBytes))
                        .build());
        if (formsMode) {
            requestBuilder.featureTypes(FeatureType.TABLES, FeatureType.FORMS);
        } else {
            requestBuilder.featureTypes(FeatureType.TABLES);
        }
        AnalyzeDocumentResponse response = callTextractThrottled(requestBuilder.build());

        String text = response.blocks().stream()
                .filter(b -> b.blockType() == BlockType.LINE)
                .map(Block::text)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining("\n"));

        if (text.isBlank()) {
            log.info("OCR completed but produced empty text — marking EMPTY_TEXT");
            return OcrResult.failure(ExtractionFailureReason.EMPTY_TEXT);
        }

        int pageCount = response.blocks().stream()
                .filter(b -> b.page() != null)
                .mapToInt(Block::page)
                .max()
                .orElse(1);

        log.info("OCR succeeded (direct) — {} chars extracted from {} page(s)", text.length(), pageCount);
        return OcrResult.success(text, pageCount);
    }

    /**
     * SF-122-08 : fallback rasterisation. Render chaque page du PDF en PNG
     * gray 200 DPI via PDFBox, puis envoie chaque PNG à Textract séparément.
     * Agrège le texte de toutes les pages réussies avec séparateur `\n\n`.
     * Si une page échoue (Textract exception), skip + continue. Si toutes
     * échouent, retourne OCR_FAILED.
     */
    private OcrResult callTextractRasterized(byte[] fileBytes, boolean formsMode, int totalPages) {
        try (PDDocument pdf = Loader.loadPDF(fileBytes)) {
            PDFRenderer renderer = new PDFRenderer(pdf);
            StringBuilder fullText = new StringBuilder();
            int successfulPages = 0;

            for (int pageIdx = 0; pageIdx < pdf.getNumberOfPages(); pageIdx++) {
                try {
                    BufferedImage image = renderer.renderImageWithDPI(pageIdx, 200, ImageType.GRAY);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "PNG", baos);
                    byte[] pngBytes = baos.toByteArray();

                    AnalyzeDocumentRequest.Builder req = AnalyzeDocumentRequest.builder()
                            .document(Document.builder()
                                    .bytes(SdkBytes.fromByteArray(pngBytes))
                                    .build());
                    if (formsMode) {
                        req.featureTypes(FeatureType.TABLES, FeatureType.FORMS);
                    } else {
                        req.featureTypes(FeatureType.TABLES);
                    }
                    AnalyzeDocumentResponse resp = callTextractThrottled(req.build());

                    String pageText = resp.blocks().stream()
                            .filter(b -> b.blockType() == BlockType.LINE)
                            .map(Block::text)
                            .filter(t -> t != null && !t.isBlank())
                            .collect(Collectors.joining("\n"));

                    if (!pageText.isBlank()) {
                        if (fullText.length() > 0) fullText.append("\n\n");
                        fullText.append(pageText);
                        successfulPages++;
                    }
                    log.info("OCR rasterized page {}/{} — {} chars", pageIdx + 1, totalPages, pageText.length());
                } catch (Exception pageErr) {
                    log.warn("OCR rasterized page {}/{} failed — class={}, message={}",
                            pageIdx + 1, totalPages, pageErr.getClass().getSimpleName(), pageErr.getMessage());
                    // Continue avec la page suivante
                }
            }

            if (fullText.length() == 0) {
                log.error("OCR rasterization produced zero text across {} pages", totalPages);
                return OcrResult.failure(ExtractionFailureReason.OCR_FAILED);
            }

            log.info("OCR succeeded (rasterized) — {} chars extracted from {}/{} page(s)",
                    fullText.length(), successfulPages, totalPages);
            return OcrResult.successRasterized(fullText.toString(), totalPages);
        } catch (Exception e) {
            log.error("OCR rasterization failed — class={}, message={}", e.getClass().getSimpleName(), e.getMessage());
            return OcrResult.failure(ExtractionFailureReason.OCR_FAILED);
        }
    }

    /**
     * SF-122-09 : wrap autour de {@code textractClient.analyzeDocument} avec
     * semaphore. Bloque en attente d'un permit si trop de calls concurrents
     * sont en vol. Libère dans un finally pour garantir l'équilibre.
     *
     * Interruption pendant l'acquire → InterruptedException propagée en
     * RuntimeException (catchée par le catch global de tryOcr → OCR_FAILED).
     */
    private AnalyzeDocumentResponse callTextractThrottled(AnalyzeDocumentRequest request) {
        try {
            textractSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Textract semaphore", e);
        }
        try {
            return textractClient.analyzeDocument(request);
        } finally {
            textractSemaphore.release();
        }
    }

    /** Compte le nombre de pages d'un PDF via PDFBox. Renvoie 1 si échec de parsing. */
    private int countPdfPages(byte[] fileBytes) {
        try (PDDocument doc = Loader.loadPDF(fileBytes)) {
            return doc.getNumberOfPages();
        } catch (Exception e) {
            return 1; // fallback conservateur — laisse Textract décider
        }
    }

    /**
     * Bean {@link TextractClient} créé uniquement si {@code aws.textract.enabled=true}.
     * En dev/test, le bean est absent → {@code OcrService} reçoit Optional.empty()
     * et court-circuite l'appel AWS.
     */
    @Configuration
    static class TextractClientConfig {

        @Bean
        @ConditionalOnProperty(prefix = "aws.textract", name = "enabled", havingValue = "true")
        TextractClient textractClient(OcrProperties properties) {
            // SF-122-07 fix : RetryMode.ADAPTIVE = token bucket client-side rate
            // limiter + backoff exponentiel sur ThrottlingException /
            // ProvisionedThroughputExceededException. 5 tentatives max. Essentiel
            // pour les retries massifs où 9 docs partent en parallèle chez Textract.
            return TextractClient.builder()
                    .region(Region.of(properties.region()))
                    .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .retryPolicy(RetryPolicy.builder(RetryMode.ADAPTIVE)
                                    .numRetries(5)
                                    .build())
                            .build())
                    .build();
        }
    }
}
