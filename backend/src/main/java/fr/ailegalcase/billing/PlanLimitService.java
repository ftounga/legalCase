package fr.ailegalcase.billing;

import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.chat.ChatMessageRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Service
public class PlanLimitService {

    // ── Dossiers ouverts ─────────────────────────────────────────────────
    private static final int FREE_MAX_OPEN_CASE_FILES  = 2;
    private static final int SOLO_MAX_OPEN_CASE_FILES  = 15;
    private static final int TEAM_MAX_OPEN_CASE_FILES  = 40;

    // ── Documents par dossier ────────────────────────────────────────────
    private static final int FREE_MAX_DOCUMENTS_PER_CASE_FILE  = 5;
    private static final int SOLO_MAX_DOCUMENTS_PER_CASE_FILE  = 15;
    private static final int TEAM_MAX_DOCUMENTS_PER_CASE_FILE  = 30;
    private static final int PRO_MAX_DOCUMENTS_PER_CASE_FILE   = 50;

    // ── Analyses par dossier ─────────────────────────────────────────────
    static final int FREE_MAX_CASE_ANALYSES_PER_CASE_FILE  = 2;
    static final int SOLO_MAX_CASE_ANALYSES_PER_CASE_FILE  = 8;
    static final int TEAM_MAX_CASE_ANALYSES_PER_CASE_FILE  = 15;

    // ── Re-analyses enrichies par dossier ────────────────────────────────
    static final int FREE_MAX_RE_ANALYSES_PER_CASE_FILE  = 1;
    static final int SOLO_MAX_RE_ANALYSES_PER_CASE_FILE  = 3;
    static final int TEAM_MAX_RE_ANALYSES_PER_CASE_FILE  = 8;

    // ── Budget tokens mensuel ────────────────────────────────────────────
    static final long FREE_MONTHLY_TOKEN_BUDGET  =  500_000L;
    static final long SOLO_MONTHLY_TOKEN_BUDGET  =  6_000_000L;
    static final long TEAM_MONTHLY_TOKEN_BUDGET  = 18_000_000L;
    static final long PRO_MONTHLY_TOKEN_BUDGET   = 60_000_000L;

    // ── Messages chat mensuels ───────────────────────────────────────────
    static final long FREE_MONTHLY_CHAT_LIMIT  =   10L;
    static final long SOLO_MONTHLY_CHAT_LIMIT  =  100L;
    static final long TEAM_MONTHLY_CHAT_LIMIT  =  300L;
    static final long PRO_MONTHLY_CHAT_LIMIT   = 1000L;

    // ── Seats facturables (SF-123-02) ────────────────────────────────────
    // Seats inclus dans le prix de base du plan (tranche 0 € sur Stripe tiered).
    static final int FREE_INCLUDED_SEATS = 1;
    static final int SOLO_INCLUDED_SEATS = 1;
    static final int TEAM_INCLUDED_SEATS = 3;
    static final int PRO_INCLUDED_SEATS  = 5;

    // Cap absolu de seats par plan (Integer.MAX_VALUE = illimité).
    static final int FREE_MAX_SEATS = 1;
    static final int SOLO_MAX_SEATS = 1;
    static final int TEAM_MAX_SEATS = 6;
    static final int PRO_MAX_SEATS  = Integer.MAX_VALUE;

    // Prix mensuels en centimes TTC (aligne avec Stripe Dashboard V2 tiered pricing).
    static final int FREE_BASE_MONTHLY_CENTS = 0;
    static final int SOLO_BASE_MONTHLY_CENTS = 9900;   // 99 €
    static final int TEAM_BASE_MONTHLY_CENTS = 21900;  // 219 €
    static final int PRO_BASE_MONTHLY_CENTS  = 42900;  // 429 €

    static final int FREE_EXTRA_SEAT_CENTS = 0;
    static final int SOLO_EXTRA_SEAT_CENTS = 0;        // pas de seat supp sur SOLO
    static final int TEAM_EXTRA_SEAT_CENTS = 5900;     // +59 €/user
    static final int PRO_EXTRA_SEAT_CENTS  = 7900;     // +79 €/user

    // ── Quotas OCR (SF-122-02) ───────────────────────────────────────────
    static final int FREE_MONTHLY_OCR_PAGES =    100;
    static final int SOLO_MONTHLY_OCR_PAGES =    800;
    static final int TEAM_MONTHLY_OCR_PAGES =  3_000;
    static final int PRO_MONTHLY_OCR_PAGES  = 10_000;
    /** Hard cap journalier anti-abus, tous plans confondus. */
    static final int DAILY_OCR_PAGES_HARD_CAP = 500;

    // ── Quotas vidéo mensuels (SF-231-03) ────────────────────────────────
    /**
     * Plafond mensuel de minutes vidéo consommées par workspace selon le plan.
     * Calcul minutes = {@code Math.ceil(durationSeconds / 60.0)} (1 s = 1 min facturée).
     * Reset au 1er du mois calendaire (UTC), pas de report.
     */
    static final int TRIAL_MONTHLY_VIDEO_MINUTES =     1;
    static final int FREE_MONTHLY_VIDEO_MINUTES  =     1;
    static final int SOLO_MONTHLY_VIDEO_MINUTES  =     5;
    static final int TEAM_MONTHLY_VIDEO_MINUTES  =    30;
    static final int PRO_MONTHLY_VIDEO_MINUTES   =   120;
    /** ENTERPRISE / plan inconnu non facturé : illimité. */
    static final int ENTERPRISE_MONTHLY_VIDEO_MINUTES = Integer.MAX_VALUE;

    private final SubscriptionRepository subscriptionRepository;
    private final UsageEventRepository usageEventRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CreditPurchaseService creditPurchaseService;
    private final WorkspaceRepository workspaceRepository;

    public PlanLimitService(SubscriptionRepository subscriptionRepository,
                            UsageEventRepository usageEventRepository,
                            ChatMessageRepository chatMessageRepository,
                            CreditPurchaseService creditPurchaseService,
                            WorkspaceRepository workspaceRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.usageEventRepository = usageEventRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.creditPurchaseService = creditPurchaseService;
        this.workspaceRepository = workspaceRepository;
    }

    public boolean isExpiredFree(Subscription sub) {
        return "FREE".equals(sub.getPlanCode())
                && sub.getExpiresAt() != null
                && Instant.now().isAfter(sub.getExpiresAt());
    }

    // ── Dossiers ouverts ─────────────────────────────────────────────────

    public int getMaxOpenCaseFiles(String planCode) {
        return switch (planCode) {
            case "PRO"  -> Integer.MAX_VALUE;
            case "TEAM" -> TEAM_MAX_OPEN_CASE_FILES;
            case "SOLO" -> SOLO_MAX_OPEN_CASE_FILES;
            default     -> FREE_MAX_OPEN_CASE_FILES;
        };
    }

    public int getMaxOpenCaseFilesForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> isExpiredFree(sub) ? 0 : getMaxOpenCaseFiles(sub.getPlanCode()))
                .orElse(Integer.MAX_VALUE);
    }

    // ── Documents par dossier ────────────────────────────────────────────

    public int getMaxDocumentsPerCaseFile(String planCode) {
        return switch (planCode) {
            case "PRO"  -> PRO_MAX_DOCUMENTS_PER_CASE_FILE;
            case "TEAM" -> TEAM_MAX_DOCUMENTS_PER_CASE_FILE;
            case "SOLO" -> SOLO_MAX_DOCUMENTS_PER_CASE_FILE;
            default     -> FREE_MAX_DOCUMENTS_PER_CASE_FILE;
        };
    }

    public int getMaxDocumentsPerCaseFileForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> isExpiredFree(sub) ? 0 : getMaxDocumentsPerCaseFile(sub.getPlanCode()))
                .orElse(Integer.MAX_VALUE);
    }

    // ── Analyses par dossier ─────────────────────────────────────────────

    public boolean isCaseAnalysisLimitReached(UUID caseFileId, UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> {
                    if (isExpiredFree(sub)) return true;
                    int max = switch (sub.getPlanCode()) {
                        case "PRO"  -> Integer.MAX_VALUE;
                        case "TEAM" -> TEAM_MAX_CASE_ANALYSES_PER_CASE_FILE;
                        case "SOLO" -> SOLO_MAX_CASE_ANALYSES_PER_CASE_FILE;
                        default     -> FREE_MAX_CASE_ANALYSES_PER_CASE_FILE;
                    };
                    if (max == Integer.MAX_VALUE) return false;
                    long count = usageEventRepository.countByCaseFileIdAndEventType(caseFileId, JobType.CASE_ANALYSIS);
                    return count >= max;
                })
                .orElse(false);
    }

    // ── Re-analyses enrichies ────────────────────────────────────────────

    public boolean isReAnalysisLimitReached(UUID caseFileId, UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> {
                    if (isExpiredFree(sub)) return true;
                    int max = switch (sub.getPlanCode()) {
                        case "PRO"  -> Integer.MAX_VALUE;
                        case "TEAM" -> TEAM_MAX_RE_ANALYSES_PER_CASE_FILE;
                        case "SOLO" -> SOLO_MAX_RE_ANALYSES_PER_CASE_FILE;
                        default     -> FREE_MAX_RE_ANALYSES_PER_CASE_FILE;
                    };
                    if (max == Integer.MAX_VALUE) return false;
                    long count = usageEventRepository.countByCaseFileIdAndEventType(
                            caseFileId, JobType.ENRICHED_ANALYSIS);
                    return count >= max;
                })
                .orElse(false);
    }

    // ── Budget tokens mensuel ────────────────────────────────────────────

    long getMonthlyTokenBudget(String planCode) {
        return switch (planCode) {
            case "PRO"  -> PRO_MONTHLY_TOKEN_BUDGET;
            case "TEAM" -> TEAM_MONTHLY_TOKEN_BUDGET;
            case "SOLO" -> SOLO_MONTHLY_TOKEN_BUDGET;
            default     -> FREE_MONTHLY_TOKEN_BUDGET;
        };
    }

    public boolean isMonthlyTokenBudgetExceeded(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> {
                    long planBudget = getMonthlyTokenBudget(sub.getPlanCode());
                    Instant startOfMonth = Instant.now()
                            .atOffset(ZoneOffset.UTC)
                            .with(TemporalAdjusters.firstDayOfMonth())
                            .withHour(0).withMinute(0).withSecond(0).withNano(0)
                            .toInstant();
                    long thisMonthUsed = usageEventRepository.sumTokensByWorkspaceIdSince(workspaceId, startOfMonth);
                    long creditsRemaining = computeCreditsRemaining(workspaceId, sub, planBudget);
                    return creditsRemaining > 0
                            ? thisMonthUsed > planBudget + creditsRemaining
                            : thisMonthUsed >= planBudget;
                })
                .orElse(false);
    }

    public long getMonthlyTokenBudgetForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> {
                    long planBudget = getMonthlyTokenBudget(sub.getPlanCode());
                    long creditsRemaining = computeCreditsRemaining(workspaceId, sub, planBudget);
                    return planBudget + creditsRemaining;
                })
                .orElse(0L);
    }

    private long computeCreditsRemaining(UUID workspaceId, Subscription sub, long planBudget) {
        long totalBought = creditPurchaseService.getTotalTokensBought(workspaceId);
        if (totalBought == 0) return 0L;
        long allTimeUsed = usageEventRepository.sumTokensByWorkspaceIdAllTime(workspaceId);
        Instant start = sub.getStartedAt() != null ? sub.getStartedAt() : Instant.now();
        long monthsActive = Math.max(1, ChronoUnit.MONTHS.between(
                start.atOffset(ZoneOffset.UTC),
                Instant.now().atOffset(ZoneOffset.UTC)) + 1);
        long allTimePlanBudget = monthsActive * planBudget;
        long creditsConsumed = Math.max(0, allTimeUsed - allTimePlanBudget);
        return Math.max(0, totalBought - creditsConsumed);
    }

    // ── Chat mensuel ─────────────────────────────────────────────────────

    long getMonthlyChatLimit(String planCode) {
        return switch (planCode) {
            case "PRO"  -> PRO_MONTHLY_CHAT_LIMIT;
            case "TEAM" -> TEAM_MONTHLY_CHAT_LIMIT;
            case "SOLO" -> SOLO_MONTHLY_CHAT_LIMIT;
            default     -> FREE_MONTHLY_CHAT_LIMIT;
        };
    }

    public boolean isChatMessageLimitReached(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> {
                    if (isExpiredFree(sub)) return true;
                    long limit = getMonthlyChatLimit(sub.getPlanCode());
                    Instant startOfMonth = Instant.now()
                            .atOffset(ZoneOffset.UTC)
                            .with(TemporalAdjusters.firstDayOfMonth())
                            .withHour(0).withMinute(0).withSecond(0).withNano(0)
                            .toInstant();
                    long used = chatMessageRepository.countByWorkspaceIdSince(workspaceId, startOfMonth);
                    return used >= limit;
                })
                .orElse(false);
    }

    // ── Chat enrichi (Sonnet) ────────────────────────────────────────────
    // Disponible sur tous les plans payants non expirés

    public boolean isEnrichedAnalysisAllowedForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> !isExpiredFree(sub) && !"FREE".equals(sub.getPlanCode()))
                .orElse(true);
    }

    // ── Quotas OCR (SF-122-02) ───────────────────────────────────────────

    public int getMonthlyOcrPages(String planCode) {
        return switch (planCode) {
            case "PRO"  -> PRO_MONTHLY_OCR_PAGES;
            case "TEAM" -> TEAM_MONTHLY_OCR_PAGES;
            case "SOLO" -> SOLO_MONTHLY_OCR_PAGES;
            default     -> FREE_MONTHLY_OCR_PAGES;
        };
    }

    /**
     * SF-122-02 + SF-122-04 : vérifie si consommer {@code additionalPages} dépasserait
     * le quota mensuel (plan + packs achetés restants) OU le hard cap journalier.
     *
     * Reliquat packs (SF-122-04) : les packs OCR achetés étendent le quota mensuel.
     * Le reliquat est calculé en soustrayant du total acheté ce qui a déjà été
     * consommé au-delà du quota plan cumulé sur toute la vie du workspace.
     *
     * Compteurs "stale" : si {@code ocr_usage_last_reset_date} est dans un mois
     * passé, le current_month est traité comme 0 (l'UPDATE suivant le réinitialisera).
     *
     * @return true si l'ajout dépasserait l'un des deux gates
     */
    public boolean isOcrQuotaExceeded(UUID workspaceId, int additionalPages) {
        if (additionalPages <= 0) return false;
        return workspaceRepository.findById(workspaceId)
                .map(ws -> {
                    String planCode = subscriptionRepository.findByWorkspaceId(workspaceId)
                            .map(Subscription::getPlanCode)
                            .orElse("FREE");
                    int monthlyLimit = getMonthlyOcrPages(planCode);
                    LocalDate today = LocalDate.now();
                    int effectiveMonth = effectiveMonthlyUsage(ws, today);
                    int effectiveDay = effectiveDailyUsage(ws, today);
                    int packsRemaining = computeOcrPacksRemaining(workspaceId, ws, planCode);
                    int effectiveMonthlyLimit = monthlyLimit + packsRemaining;
                    return (effectiveDay + additionalPages > DAILY_OCR_PAGES_HARD_CAP)
                            || (effectiveMonth + additionalPages > effectiveMonthlyLimit);
                })
                .orElse(true); // workspace introuvable → block par défaut
    }

    /**
     * SF-122-04 : calcule les pages OCR encore disponibles depuis les packs achetés.
     * Formule : totalBought - max(0, allTimeUsed - monthsActive × planMonthly).
     * Pattern identique à {@code computeCreditsRemaining} pour les tokens.
     */
    public int computeOcrPacksRemaining(UUID workspaceId, Workspace ws, String planCode) {
        long totalBought = creditPurchaseService.getTotalOcrPagesBought(workspaceId);
        if (totalBought == 0) return 0;
        long allTimeUsed = ws.getOcrPagesUsedAllTime();
        long monthlyPlan = getMonthlyOcrPages(planCode);
        // Approximation conservatrice : 1 mois actif. Si on veut plus précis,
        // utiliser subscription.startedAt et compter les mois. Pour V1, OK.
        long consumedBeyondPlan = Math.max(0, allTimeUsed - monthlyPlan);
        return (int) Math.max(0, totalBought - consumedBeyondPlan);
    }

    /** SF-122-04 : somme des pages OCR achetées (exposé pour l'UI billing). */
    public long getOcrPagesBought(UUID workspaceId) {
        return creditPurchaseService.getTotalOcrPagesBought(workspaceId);
    }

    // ── Quotas vidéo mensuels (SF-231-03) ────────────────────────────────

    /**
     * SF-231-03 : minutes vidéo autorisées pour le mois calendaire courant
     * pour ce code de plan.
     *
     * @param planCode SOLO / TEAM / PRO / TRIAL / ENTERPRISE / FREE
     * @return plafond mensuel ; {@link Integer#MAX_VALUE} pour ENTERPRISE / plan inconnu non facturé.
     */
    public int getMonthlyVideoMinutes(String planCode) {
        if (planCode == null) return FREE_MONTHLY_VIDEO_MINUTES;
        return switch (planCode) {
            case "PRO"        -> PRO_MONTHLY_VIDEO_MINUTES;
            case "TEAM"       -> TEAM_MONTHLY_VIDEO_MINUTES;
            case "SOLO"       -> SOLO_MONTHLY_VIDEO_MINUTES;
            case "TRIAL"      -> TRIAL_MONTHLY_VIDEO_MINUTES;
            case "ENTERPRISE" -> ENTERPRISE_MONTHLY_VIDEO_MINUTES;
            default           -> FREE_MONTHLY_VIDEO_MINUTES;
        };
    }

    /**
     * SF-231-03 : minutes vidéo autorisées pour le workspace (selon son plan actif).
     * Aucune ligne d'abonnement → {@link #FREE_MONTHLY_VIDEO_MINUTES} (gate strict).
     */
    public int getMonthlyVideoMinutesForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> getMonthlyVideoMinutes(sub.getPlanCode()))
                .orElse(FREE_MONTHLY_VIDEO_MINUTES);
    }

    // ── Seats par plan (SF-123-02) ───────────────────────────────────────

    public int getIncludedSeats(String planCode) {
        return switch (planCode) {
            case "PRO"  -> PRO_INCLUDED_SEATS;
            case "TEAM" -> TEAM_INCLUDED_SEATS;
            case "SOLO" -> SOLO_INCLUDED_SEATS;
            default     -> FREE_INCLUDED_SEATS;
        };
    }

    public int getMaxSeats(String planCode) {
        return switch (planCode) {
            case "PRO"  -> PRO_MAX_SEATS;
            case "TEAM" -> TEAM_MAX_SEATS;
            case "SOLO" -> SOLO_MAX_SEATS;
            default     -> FREE_MAX_SEATS;
        };
    }

    public int getMaxSeatsForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> isExpiredFree(sub) ? 0 : getMaxSeats(sub.getPlanCode()))
                .orElse(FREE_MAX_SEATS);
    }

    public String getPlanCodeForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(Subscription::getPlanCode)
                .orElse("FREE");
    }

    public int getBaseMonthlyCostCents(String planCode) {
        return switch (planCode) {
            case "PRO"  -> PRO_BASE_MONTHLY_CENTS;
            case "TEAM" -> TEAM_BASE_MONTHLY_CENTS;
            case "SOLO" -> SOLO_BASE_MONTHLY_CENTS;
            default     -> FREE_BASE_MONTHLY_CENTS;
        };
    }

    public int getExtraSeatPriceCents(String planCode) {
        return switch (planCode) {
            case "PRO"  -> PRO_EXTRA_SEAT_CENTS;
            case "TEAM" -> TEAM_EXTRA_SEAT_CENTS;
            case "SOLO" -> SOLO_EXTRA_SEAT_CENTS;
            default     -> FREE_EXTRA_SEAT_CENTS;
        };
    }

    public static int effectiveMonthlyUsage(Workspace ws, LocalDate today) {
        LocalDate lastReset = ws.getOcrUsageLastResetDate();
        if (lastReset == null) return 0;
        boolean sameMonth = lastReset.getYear() == today.getYear()
                && lastReset.getMonth() == today.getMonth();
        return sameMonth ? ws.getOcrPagesUsedCurrentMonth() : 0;
    }

    public static int effectiveDailyUsage(Workspace ws, LocalDate today) {
        LocalDate lastReset = ws.getOcrUsageLastResetDate();
        if (lastReset == null) return 0;
        return lastReset.equals(today) ? ws.getOcrPagesUsedCurrentDay() : 0;
    }
}
