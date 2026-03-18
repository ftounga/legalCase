package fr.ailegalcase.billing;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PlanLimitService {

    private static final int STARTER_MAX_OPEN_CASE_FILES = 3;
    private static final int PRO_MAX_OPEN_CASE_FILES = 20;
    private static final int STARTER_MAX_DOCUMENTS_PER_CASE_FILE = 5;
    private static final int PRO_MAX_DOCUMENTS_PER_CASE_FILE = 30;

    private final SubscriptionRepository subscriptionRepository;

    public PlanLimitService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public int getMaxOpenCaseFiles(String planCode) {
        return "PRO".equals(planCode) ? PRO_MAX_OPEN_CASE_FILES : STARTER_MAX_OPEN_CASE_FILES;
    }

    public int getMaxOpenCaseFilesForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> getMaxOpenCaseFiles(sub.getPlanCode()))
                .orElse(Integer.MAX_VALUE);
    }

    public int getMaxDocumentsPerCaseFile(String planCode) {
        return "PRO".equals(planCode) ? PRO_MAX_DOCUMENTS_PER_CASE_FILE : STARTER_MAX_DOCUMENTS_PER_CASE_FILE;
    }

    public int getMaxDocumentsPerCaseFileForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> getMaxDocumentsPerCaseFile(sub.getPlanCode()))
                .orElse(Integer.MAX_VALUE);
    }

    public boolean isEnrichedAnalysisAllowed(String planCode) {
        return "PRO".equals(planCode);
    }

    public boolean isEnrichedAnalysisAllowedForWorkspace(UUID workspaceId) {
        return subscriptionRepository.findByWorkspaceId(workspaceId)
                .map(sub -> isEnrichedAnalysisAllowed(sub.getPlanCode()))
                .orElse(true);
    }
}
