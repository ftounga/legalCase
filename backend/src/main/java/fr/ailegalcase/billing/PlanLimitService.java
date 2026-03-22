package fr.ailegalcase.billing;

import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.UsageEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PlanLimitService {

    private static final int FREE_MAX_OPEN_CASE_FILES = 1;
    private static final int STARTER_MAX_OPEN_CASE_FILES = 3;
    private static final int PRO_MAX_OPEN_CASE_FILES = 20;
    private static final int FREE_MAX_DOCUMENTS_PER_CASE_FILE = 3;
    private static final int STARTER_MAX_DOCUMENTS_PER_CASE_FILE = 5;
    private static final int PRO_MAX_DOCUMENTS_PER_CASE_FILE = 30;
    static final int PRO_MAX_RE_ANALYSES_PER_CASE_FILE = 5;

    private final SubscriptionRepository subscriptionRepository;
    private final UsageEventRepository usageEventRepository;

    public PlanLimitService(SubscriptionRepository subscriptionRepository,
                            UsageEventRepository usageEventRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.usageEventRepository = usageEventRepository;
    }

    public int getMaxOpenCaseFiles(String planCode) {
        if ("PRO".equals(planCode)) return PRO_MAX_OPEN_CASE_FILES;
        if ("FREE".equals(planCode)) return FREE_MAX_OPEN_CASE_FILES;
        return STARTER_MAX_OPEN_CASE_FILES;
    }

    public boolean isExpiredFree(Subscription sub) {
        return "FREE".equals(sub.getPlanCode())
                && sub.getExpiresAt() != null
                && Instant.now().isAfter(sub.getExpiresAt());
    }

    public int getMaxOpenCaseFilesForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> isExpiredFree(sub) ? 0 : getMaxOpenCaseFiles(sub.getPlanCode()))
                .orElse(Integer.MAX_VALUE);
    }

    public int getMaxDocumentsPerCaseFile(String planCode) {
        if ("PRO".equals(planCode)) return PRO_MAX_DOCUMENTS_PER_CASE_FILE;
        if ("FREE".equals(planCode)) return FREE_MAX_DOCUMENTS_PER_CASE_FILE;
        return STARTER_MAX_DOCUMENTS_PER_CASE_FILE;
    }

    public int getMaxDocumentsPerCaseFileForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> isExpiredFree(sub) ? 0 : getMaxDocumentsPerCaseFile(sub.getPlanCode()))
                .orElse(Integer.MAX_VALUE);
    }

    public boolean isReAnalysisLimitReached(UUID caseFileId, UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> {
                    if (!"PRO".equals(sub.getPlanCode())) return false;
                    long count = usageEventRepository.countByCaseFileIdAndEventType(
                            caseFileId, JobType.ENRICHED_ANALYSIS);
                    return count >= PRO_MAX_RE_ANALYSES_PER_CASE_FILE;
                })
                .orElse(false);
    }

    public boolean isEnrichedAnalysisAllowed(String planCode) {
        return "PRO".equals(planCode);
    }

    public boolean isEnrichedAnalysisAllowedForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> !isExpiredFree(sub) && isEnrichedAnalysisAllowed(sub.getPlanCode()))
                .orElse(true);
    }
}
