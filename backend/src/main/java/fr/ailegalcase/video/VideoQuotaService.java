package fr.ailegalcase.video;

import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.shared.PaymentRequiredCode;
import fr.ailegalcase.shared.PaymentRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * SF-231-03 — Quota mensuel minutes vidéo par workspace, calculé selon le plan
 * actif. Pattern miroir des quotas OCR (F-122) avec un INSERT par upload dans
 * {@link WorkspaceVideoUsage} (vs compteur agrégé).
 *
 * <p>Plafonds (cf. {@link PlanLimitService#getMonthlyVideoMinutes(String)}) :
 * SOLO 5 min, TEAM 30 min, PRO 120 min, TRIAL 1 min, ENTERPRISE illimité.
 * Calcul minutes = {@code Math.ceil(durationSeconds / 60.0)} (1 s consommée =
 * 1 min facturée).
 *
 * <p>Cycle d'appel par {@code DocumentService.upload(...)} :
 * <ol>
 *   <li>{@link #checkVideoQuotaForUpload(UUID, long)} AVANT toute persistance —
 *       permet d'éviter d'écrire un blob orphelin S3 si le quota est dépassé ;
 *   <li>après {@code documentRepository.save(document)},
 *       {@link #recordVideoUsage(UUID, UUID, long)} INSERT la ligne avec le
 *       {@code documentId} valide (FK respectée).
 * </ol>
 *
 * <p>Race condition : la transaction {@link #recordVideoUsage} est
 * {@link Isolation#REPEATABLE_READ} — la requête sum + l'INSERT voient un
 * snapshot cohérent ; deux uploads concurrents qui frôlent la limite sont
 * arbitrés au commit, et la seconde transaction lève 402 si dépassement
 * effectif. Le pré-check {@link #checkVideoQuotaForUpload} est best-effort
 * (read-only, peut sous-estimer la consommation concurrente) — la vérification
 * de référence est dans {@link #recordVideoUsage}.
 */
@Service
public class VideoQuotaService {

    private static final Logger log = LoggerFactory.getLogger(VideoQuotaService.class);

    private final WorkspaceVideoUsageRepository repository;
    private final PlanLimitService planLimitService;
    private final Clock clock;

    @Autowired
    public VideoQuotaService(WorkspaceVideoUsageRepository repository,
                             PlanLimitService planLimitService) {
        this(repository, planLimitService, Clock.systemUTC());
    }

    /** Constructeur exposé pour testabilité (clock injectable). */
    VideoQuotaService(WorkspaceVideoUsageRepository repository,
                      PlanLimitService planLimitService,
                      Clock clock) {
        this.repository = repository;
        this.planLimitService = planLimitService;
        this.clock = clock;
    }

    /**
     * Pré-vérification du quota AVANT upload (avant écriture S3).
     * Évite les blobs orphelins quand le quota est manifestement dépassé.
     *
     * <p>Lecture seule, peut sous-estimer la consommation concurrente — la
     * vérification de référence est faite dans
     * {@link #recordVideoUsage(UUID, UUID, long)} sous transaction
     * {@code REPEATABLE_READ}.
     *
     * @throws PaymentRequiredException avec {@link PaymentRequiredCode#VIDEO_QUOTA_EXCEEDED}
     */
    @Transactional(readOnly = true)
    public void checkVideoQuotaForUpload(UUID workspaceId, long durationSeconds) {
        if (workspaceId == null) {
            // Cas défensif : pas de workspace → on n'autorise pas la consommation vidéo.
            throw new PaymentRequiredException(PaymentRequiredCode.VIDEO_QUOTA_EXCEEDED,
                    "Workspace introuvable — quota vidéo refusé.");
        }

        int planLimit = planLimitService.getMonthlyVideoMinutesForWorkspace(workspaceId);
        if (planLimit == Integer.MAX_VALUE) {
            return; // ENTERPRISE : illimité.
        }

        int minutesToAdd = computeMinutesFromSeconds(durationSeconds);
        if (minutesToAdd <= 0) return; // durée = 0 ⇒ rien à consommer ⇒ OK.

        String month = currentYearMonth();
        int alreadyConsumed = repository.sumMinutesByWorkspaceAndMonth(workspaceId, month);
        if (alreadyConsumed + minutesToAdd > planLimit) {
            throw quotaExceeded(planLimit, alreadyConsumed, minutesToAdd);
        }
    }

    /**
     * Enregistre la consommation après persistance du document. Re-vérifie le
     * quota sous {@link Isolation#REPEATABLE_READ} pour arbitrer les uploads
     * concurrents — la première transaction qui commit gagne, la seconde lève
     * 402 si dépassement effectif.
     *
     * @throws PaymentRequiredException avec {@link PaymentRequiredCode#VIDEO_QUOTA_EXCEEDED}
     *         si le quota est dépassé entre le pré-check et le commit.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void recordVideoUsage(UUID workspaceId, UUID documentId, long durationSeconds) {
        if (workspaceId == null || documentId == null) {
            throw new IllegalArgumentException(
                    "workspaceId et documentId sont obligatoires pour recordVideoUsage");
        }

        int minutesToAdd = computeMinutesFromSeconds(durationSeconds);
        if (minutesToAdd <= 0) {
            // Durée nulle ou négative : pas de consommation à enregistrer.
            log.debug("recordVideoUsage skipped — durationSeconds={}, minutesToAdd=0",
                    durationSeconds);
            return;
        }

        int planLimit = planLimitService.getMonthlyVideoMinutesForWorkspace(workspaceId);
        String month = currentYearMonth();

        if (planLimit != Integer.MAX_VALUE) {
            int alreadyConsumed = repository.sumMinutesByWorkspaceAndMonth(workspaceId, month);
            if (alreadyConsumed + minutesToAdd > planLimit) {
                throw quotaExceeded(planLimit, alreadyConsumed, minutesToAdd);
            }
        }

        WorkspaceVideoUsage row = new WorkspaceVideoUsage();
        row.setWorkspaceId(workspaceId);
        row.setDocumentId(documentId);
        row.setYearMonth(month);
        row.setMinutesConsumed(minutesToAdd);
        repository.save(row);

        log.info("Video usage recorded — workspace={}, document={}, month={}, minutes={}",
                workspaceId, documentId, month, minutesToAdd);
    }

    /**
     * Variante atomique conforme à la mini-spec — combine pré-check + INSERT.
     * Utilisable quand l'appelant a déjà persisté le document (FK valide) et
     * souhaite une seule méthode. {@link #recordVideoUsage} est l'implémentation
     * sous-jacente, mais l'API est exposée pour cohérence avec la mini-spec.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void checkAndConsumeVideoMinutes(UUID workspaceId, long durationSeconds, UUID documentId) {
        recordVideoUsage(workspaceId, documentId, durationSeconds);
    }

    /** Visible pour les tests. {@code Math.ceil(durationSeconds/60.0)}. */
    static int computeMinutesFromSeconds(long durationSeconds) {
        if (durationSeconds <= 0) return 0;
        return (int) Math.ceil(durationSeconds / 60.0);
    }

    private String currentYearMonth() {
        return YearMonth.now(clock.withZone(ZoneOffset.UTC)).toString();
    }

    private PaymentRequiredException quotaExceeded(int planLimit, int alreadyConsumed, int requested) {
        String message = String.format(
                "Quota vidéo mensuel atteint : %d min déjà consommées + %d min demandées > %d min (plafond plan). "
                        + "Passez au plan supérieur pour débloquer plus de minutes vidéo.",
                alreadyConsumed, requested, planLimit);
        return new PaymentRequiredException(PaymentRequiredCode.VIDEO_QUOTA_EXCEEDED, message);
    }
}
