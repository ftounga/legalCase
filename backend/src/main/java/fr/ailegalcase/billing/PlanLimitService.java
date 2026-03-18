package fr.ailegalcase.billing;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PlanLimitService {

    private static final int STARTER_MAX_OPEN_CASE_FILES = 3;
    private static final int PRO_MAX_OPEN_CASE_FILES = 20;

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
}
