package fr.ailegalcase.ocr;

import fr.ailegalcase.document.ExtractionFailureReason;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeDocumentRequest;
import software.amazon.awssdk.services.textract.model.AnalyzeDocumentResponse;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.Document;
import software.amazon.awssdk.services.textract.model.FeatureType;

import java.util.Optional;
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

    public OcrService(Optional<TextractClient> textractClient, OcrProperties properties) {
        this.textractClient = textractClient.orElse(null);
        this.properties = properties;
    }

    /**
     * Tente une extraction OCR du document. Idempotent (aucun état interne).
     * Ne met pas à jour le compteur {@code ocr_pages_used} du workspace —
     * ExtractionService s'en charge via {@code WorkspaceRepository.incrementOcrUsage}.
     *
     * @param fileBytes contenu binaire du fichier
     * @return {@link OcrResult} success avec texte + pageCount, ou failure avec motif
     */
    public OcrResult tryOcr(byte[] fileBytes) {
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

        try {
            AnalyzeDocumentResponse response = textractClient.analyzeDocument(
                    AnalyzeDocumentRequest.builder()
                            .document(Document.builder()
                                    .bytes(SdkBytes.fromByteArray(fileBytes))
                                    .build())
                            .featureTypes(FeatureType.TABLES)
                            .build());

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

            log.info("OCR succeeded — {} chars extracted from {} page(s)", text.length(), pageCount);
            return OcrResult.success(text, pageCount);

        } catch (Exception e) {
            log.error("OCR failed via Textract — class={}, message={}", e.getClass().getSimpleName(), e.getMessage());
            return OcrResult.failure(ExtractionFailureReason.OCR_FAILED);
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
            return TextractClient.builder()
                    .region(Region.of(properties.region()))
                    .build();
        }
    }
}
