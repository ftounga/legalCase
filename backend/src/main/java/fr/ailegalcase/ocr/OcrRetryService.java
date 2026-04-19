package fr.ailegalcase.ocr;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentUploadedEvent;
import fr.ailegalcase.document.ExtractionFailureReason;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SF-122-05 : relance l'OCR sur les documents FAILED d'un dossier avec motif
 * éligible (EMPTY_TEXT ou OCR_FAILED). Utile pour rattraper les échecs
 * antérieurs au déploiement de F-122 (MEA AVOCATS notamment).
 *
 * Rate limit in-memory : max 1 retry par dossier / 10 min (évite l'abus).
 */
@Service
public class OcrRetryService {

    private static final Logger log = LoggerFactory.getLogger(OcrRetryService.class);
    private static final Set<ExtractionFailureReason> ELIGIBLE_REASONS =
            Set.of(ExtractionFailureReason.EMPTY_TEXT, ExtractionFailureReason.OCR_FAILED);
    private static final long RATE_LIMIT_MS = 10 * 60 * 1000L; // 10 minutes

    private final CaseFileRepository caseFileRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanLimitService planLimitService;
    private final ApplicationEventPublisher eventPublisher;

    /** Rate limit en mémoire — perdu au redémarrage, acceptable V1. */
    private final Map<UUID, Instant> lastRetryByCaseFile = new ConcurrentHashMap<>();

    public OcrRetryService(CaseFileRepository caseFileRepository,
                           DocumentExtractionRepository extractionRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           CurrentUserResolver currentUserResolver,
                           SubscriptionRepository subscriptionRepository,
                           PlanLimitService planLimitService,
                           ApplicationEventPublisher eventPublisher) {
        this.caseFileRepository = caseFileRepository;
        this.extractionRepository = extractionRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.subscriptionRepository = subscriptionRepository;
        this.planLimitService = planLimitService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public OcrRetryPreviewResponse preview(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
        CaseFile caseFile = resolveCaseFile(caseFileId, oidcUser, provider, principal);
        List<DocumentExtraction> eligible = findRetryableFiltered(caseFileId);

        int estimatedPages = eligible.stream()
                .mapToInt(e -> countPdfPagesSafe(e.getDocument()))
                .sum();

        Workspace workspace = caseFile.getWorkspace();
        String planCode = subscriptionRepository.findByWorkspaceId(workspace.getId())
                .map(Subscription::getPlanCode).orElse("FREE");
        int monthlyLimit = planLimitService.getMonthlyOcrPages(planCode);
        int packsRemaining = planLimitService.computeOcrPacksRemaining(workspace.getId(), workspace, planCode);
        int currentMonth = PlanLimitService.effectiveMonthlyUsage(workspace, java.time.LocalDate.now());
        int monthlyRemaining = Math.max(0, monthlyLimit - currentMonth);
        boolean canRetry = !eligible.isEmpty() && estimatedPages <= monthlyRemaining + packsRemaining;

        return new OcrRetryPreviewResponse(eligible.size(), estimatedPages, monthlyRemaining, packsRemaining, canRetry);
    }

    @Transactional
    public int retry(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
        CaseFile caseFile = resolveCaseFile(caseFileId, oidcUser, provider, principal);
        checkRateLimit(caseFileId);

        List<DocumentExtraction> eligible = findRetryableFiltered(caseFileId);
        if (eligible.isEmpty()) return 0;

        // Supprime les vieilles extractions FAILED éligibles
        List<UUID> docIds = eligible.stream().map(e -> e.getDocument().getId()).toList();
        extractionRepository.deleteByDocumentIdIn(docIds);

        // SF-122-07 bug fix : force ocrEnabled=true avant republish. Si l'avocat avait
        // décoché l'OCR à l'upload (ocrEnabled=false), ExtractionService skippait
        // Textract et remettait les docs en EMPTY_TEXT direct — le bouton "Relancer
        // avec OCR" ne faisait alors aucun OCR. Le clic sur ce bouton EST l'opt-in
        // explicite a posteriori.
        for (DocumentExtraction extraction : eligible) {
            Document doc = extraction.getDocument();
            if (!doc.isOcrEnabled()) {
                doc.setOcrEnabled(true);
                // La sauvegarde est gérée par la @Transactional du service (dirty checking)
            }
        }

        // Republie les events pour re-déclencher ExtractionService (qui passera par OcrService)
        for (DocumentExtraction extraction : eligible) {
            Document doc = extraction.getDocument();
            eventPublisher.publishEvent(new DocumentUploadedEvent(
                    doc.getId(), doc.getStorageKey(), doc.getContentType()));
        }
        lastRetryByCaseFile.put(caseFileId, Instant.now());
        log.info("OCR retry triggered for case {} ({} documents re-enqueued)", caseFileId, eligible.size());
        return eligible.size();
    }

    /**
     * SF-122-06 : filtre les extractions éligibles au retry en excluant les
     * cas futiles où l'OCR a déjà été tenté sans succès (EMPTY_TEXT après
     * un appel Textract qui a renvoyé 0 blocks — ré-appeler donnerait le
     * même résultat). OCR_FAILED reste toujours éligible car peut être
     * un throttle transient AWS.
     *
     * Détection via la metadata écrite par ExtractionService :
     * - "extractor":"internal"          → OCR jamais tenté (legacy avant F-122)
     * - "extractor":"internal+textract" → OCR tenté et failed
     * - "extractor":"textract"          → OCR succès (pas dans FAILED)
     */
    List<DocumentExtraction> findRetryableFiltered(UUID caseFileId) {
        return extractionRepository.findRetryableByCaseFile(caseFileId, ELIGIBLE_REASONS).stream()
                .filter(OcrRetryService::isRetryWorthAttempt)
                .toList();
    }

    static boolean isRetryWorthAttempt(DocumentExtraction e) {
        if (e.getFailureReason() == ExtractionFailureReason.OCR_FAILED) {
            return true; // transient AWS — retry peut marcher
        }
        // EMPTY_TEXT : éligible uniquement si OCR jamais tenté
        String metadata = e.getExtractionMetadata();
        boolean ocrAlreadyAttempted = metadata != null && metadata.contains("textract");
        return !ocrAlreadyAttempted;
    }

    private void checkRateLimit(UUID caseFileId) {
        Instant last = lastRetryByCaseFile.get(caseFileId);
        if (last != null && Instant.now().toEpochMilli() - last.toEpochMilli() < RATE_LIMIT_MS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Un retry OCR a déjà été lancé pour ce dossier dans les 10 dernières minutes");
        }
    }

    private CaseFile resolveCaseFile(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        return caseFile;
    }

    /** Compte les pages du PDF via PDFBox (best-effort). Pour un doc non-PDF, renvoie 1. */
    private int countPdfPagesSafe(Document doc) {
        if (!"application/pdf".equals(doc.getContentType())) return 1;
        // Sans recharger le binaire depuis S3 (coût I/O), on approxime à 1 page par doc.
        // Pour un preview V1, suffisant. Future amélioration : cacher le pageCount sur Document.
        return 1;
    }
}
