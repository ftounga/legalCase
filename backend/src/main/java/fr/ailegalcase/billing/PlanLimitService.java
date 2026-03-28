package fr.ailegalcase.billing;

import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.chat.ChatMessageRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
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

    private final SubscriptionRepository subscriptionRepository;
    private final UsageEventRepository usageEventRepository;
    private final ChatMessageRepository chatMessageRepository;

    public PlanLimitService(SubscriptionRepository subscriptionRepository,
                            UsageEventRepository usageEventRepository,
                            ChatMessageRepository chatMessageRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.usageEventRepository = usageEventRepository;
        this.chatMessageRepository = chatMessageRepository;
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
                    long budget = getMonthlyTokenBudget(sub.getPlanCode());
                    Instant startOfMonth = Instant.now()
                            .atOffset(ZoneOffset.UTC)
                            .with(TemporalAdjusters.firstDayOfMonth())
                            .withHour(0).withMinute(0).withSecond(0).withNano(0)
                            .toInstant();
                    long used = usageEventRepository.sumTokensByWorkspaceIdSince(workspaceId, startOfMonth);
                    return used >= budget;
                })
                .orElse(false);
    }

    public long getMonthlyTokenBudgetForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> getMonthlyTokenBudget(sub.getPlanCode()))
                .orElse(0L);
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
}
