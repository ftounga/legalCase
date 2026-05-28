package fr.ailegalcase.stylelearning;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.JobType;
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
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/**
 * F-98 / SF-98-46 — worker asynchrone d'extraction de la signature de style.
 *
 * <p>Consomme la queue {@code style.corpus} : télécharge le fichier S3, extrait
 * le texte, appelle {@link AnthropicService} avec le prompt de
 * {@link StyleSignaturePromptBuilder}, persiste la {@code styleSignature} +
 * statut {@code DONE} (ou {@code FAILED} + {@code errorMessage}).</p>
 *
 * <p><strong>Minimisation RGPD</strong> (invariant 1 du cadrage) : le fichier S3
 * est <strong>toujours purgé</strong> à la fin du traitement — succès ou échec —
 * et le texte brut extrait n'est <strong>jamais</strong> persisté. Seule la
 * {@code styleSignature} subsiste.</p>
 *
 * <p>Arbitrage : l'extraction de texte est faite <em>inline</em> avec POI /
 * PDFBox plutôt que via {@code ExtractionService} — ce dernier est fortement
 * couplé à l'entité {@code Document} (il crée des {@code DocumentExtraction} /
 * {@code DocumentPiece}, publie des événements, incrémente les quotas OCR) et ne
 * sait pas extraire du texte hors d'un dossier. La logique POI/PDFBox réutilisée
 * ici est strictement celle de {@code ExtractionService.extractFrom*}.</p>
 *
 * <p>Profil {@code local}/{@code prod} uniquement, aligné sur les autres workers
 * {@code @RabbitListener}. En test, le worker est instancié directement.</p>
 */
@Service
@Profile({"local", "prod"})
public class StyleCorpusExtractionService {

    private static final Logger log = LoggerFactory.getLogger(StyleCorpusExtractionService.class);

    private final StyleCorpusRepository styleCorpusRepository;
    private final StorageService storageService;
    private final AnthropicService anthropicService;
    private final StyleSignaturePromptBuilder promptBuilder;

    /** Auto-référence pour franchir le proxy transactionnel depuis le listener. */
    @Lazy
    @Autowired
    private StyleCorpusExtractionService self;

    public StyleCorpusExtractionService(StyleCorpusRepository styleCorpusRepository,
                                        StorageService storageService,
                                        AnthropicService anthropicService,
                                        StyleSignaturePromptBuilder promptBuilder) {
        this.styleCorpusRepository = styleCorpusRepository;
        this.storageService = storageService;
        this.anthropicService = anthropicService;
        this.promptBuilder = promptBuilder;
    }

    @RabbitListener(queues = StyleCorpusRabbitMQConfig.STYLE_CORPUS_QUEUE, concurrency = "2")
    public void consumeStyleCorpus(StyleCorpusMessage message) {
        process(message.styleCorpusDocumentId(), message.storageKey());
    }

    /**
     * Extrait la signature de style d'un document de corpus.
     *
     * <p>Le fichier S3 est purgé dans tous les cas, en dernier — y compris si
     * l'extraction de texte ou l'appel IA échoue (CA3 / CA7, minimisation RGPD).
     * Aucun texte brut n'est jamais persisté.</p>
     *
     * @param documentId identifiant de la ligne {@code style_corpus_documents}
     * @param storageKey clé S3 du fichier source temporaire à traiter puis purger
     */
    public void process(UUID documentId, String storageKey) {
        boolean started = self.markProcessing(documentId);
        if (!started) {
            // Ligne introuvable / déjà traitée — on purge tout de même le fichier S3.
            purgeStorage(storageKey, documentId);
            return;
        }

        String signature = null;
        Exception failure = null;
        try {
            byte[] fileBytes = storageService.download(storageKey);
            StyleCorpusDocument doc = styleCorpusRepository.findById(documentId).orElseThrow();
            String text = extractText(fileBytes, doc.getContentType(), doc.getOriginalFilename());
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Texte extrait vide — document illisible.");
            }
            AiCallContext ctx = AiCallContext.systemLevel(JobType.SYSTEM_STYLE_LEARNING);
            AnthropicResult result = anthropicService.analyzeWithSystemCache(
                    ctx,
                    promptBuilder.buildSystemPrompt(),
                    promptBuilder.buildUserMessage(text),
                    StyleSignaturePromptBuilder.MAX_TOKENS);
            if (result == null || result.content() == null || result.content().isBlank()) {
                throw new IllegalStateException("Réponse IA vide.");
            }
            signature = result.content().trim();
        } catch (Exception e) {
            log.error("Style signature extraction FAILED — document={}", documentId, e);
            failure = e;
        }

        // Persistance du résultat (le texte brut n'est jamais persisté).
        self.finalize(documentId, signature, failure);

        // Purge S3 — invariant RGPD : effectuée dans tous les cas, succès comme échec.
        purgeStorage(storageKey, documentId);
    }

    /**
     * Phase 1 — passe la ligne à {@code PROCESSING}.
     *
     * @return {@code true} si la ligne existait et était {@code PENDING},
     *         {@code false} sinon (idempotence sur double message)
     */
    @Transactional
    public boolean markProcessing(UUID documentId) {
        StyleCorpusDocument doc = styleCorpusRepository.findById(documentId).orElse(null);
        if (doc == null) {
            log.warn("Style corpus document {} introuvable — message ignoré", documentId);
            return false;
        }
        if (doc.getStatus() != StyleCorpusDocumentStatus.PENDING) {
            log.warn("Style corpus document {} déjà au statut {} — message ignoré",
                    documentId, doc.getStatus());
            return false;
        }
        doc.setStatus(StyleCorpusDocumentStatus.PROCESSING);
        styleCorpusRepository.save(doc);
        return true;
    }

    /**
     * Phase 3 — persiste le résultat : {@code DONE} + {@code styleSignature} si
     * l'extraction a réussi, {@code FAILED} + {@code errorMessage} sinon. Le texte
     * brut extrait n'est jamais persisté.
     */
    @Transactional
    public void finalize(UUID documentId, String signature, Exception failure) {
        StyleCorpusDocument doc = styleCorpusRepository.findById(documentId).orElse(null);
        if (doc == null) {
            log.warn("Style corpus document {} introuvable à la finalisation", documentId);
            return;
        }
        if (failure != null || signature == null) {
            doc.setStatus(StyleCorpusDocumentStatus.FAILED);
            doc.setErrorMessage(failure != null
                    ? truncate(failure.getMessage(), 2000)
                    : "Échec de l'extraction de la signature de style.");
            doc.setStyleSignature(null);
            styleCorpusRepository.save(doc);
            log.info("Style signature extraction marked FAILED — document={}", documentId);
            return;
        }
        doc.setStatus(StyleCorpusDocumentStatus.DONE);
        doc.setStyleSignature(signature);
        doc.setErrorMessage(null);
        styleCorpusRepository.save(doc);
        log.info("Style signature extraction DONE — document={}, {} chars", documentId, signature.length());
    }

    /**
     * Purge le fichier source S3. Fail-safe : une erreur de purge est loguée mais
     * ne fait pas retomber le traitement (le statut métier est déjà persisté).
     */
    private void purgeStorage(String storageKey, UUID documentId) {
        if (storageKey == null) {
            return;
        }
        try {
            storageService.delete(storageKey);
            log.info("Style corpus source file purged from S3 — document={}, key={}",
                    documentId, storageKey);
        } catch (Exception e) {
            log.error("Failed to purge style corpus source file {} for document {} — {}",
                    storageKey, documentId, e.getMessage());
        }
    }

    /**
     * Extrait le texte d'un fichier pdf / doc / docx / txt. Réutilise la logique
     * POI / PDFBox de {@code ExtractionService} ; le routage tolère un content
     * type générique en se rabattant sur l'extension du nom de fichier.
     */
    String extractText(byte[] fileBytes, String contentType, String filename) throws Exception {
        String resolved = resolveFormat(contentType, filename);
        return switch (resolved) {
            case "pdf" -> extractFromPdf(fileBytes);
            case "docx" -> extractFromDocx(fileBytes);
            case "doc" -> extractFromDoc(fileBytes);
            case "txt" -> new String(fileBytes, StandardCharsets.UTF_8);
            default -> throw new IllegalStateException("Format non supporté : " + contentType);
        };
    }

    /** Détermine le format (pdf/doc/docx/txt) depuis le content type, sinon l'extension. */
    private static String resolveFormat(String contentType, String filename) {
        if (contentType != null) {
            switch (contentType) {
                case "application/pdf":
                    return "pdf";
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    return "docx";
                case "application/msword":
                    return "doc";
                case "text/plain":
                    return "txt";
                default:
                    break;
            }
        }
        return extensionOf(filename);
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
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

    private static String truncate(String value, int max) {
        if (value == null) {
            return "Échec de l'extraction.";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
