package fr.ailegalcase.billing;

import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.UsageEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanLimitServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UsageEventRepository usageEventRepository;

    private PlanLimitService service;

    @BeforeEach
    void setUp() {
        service = new PlanLimitService(subscriptionRepository, usageEventRepository);
    }

    // --- getMaxOpenCaseFiles ---

    @Test
    void getMaxOpenCaseFiles_free_returns1() {
        assertThat(service.getMaxOpenCaseFiles("FREE")).isEqualTo(1);
    }

    @Test
    void getMaxOpenCaseFiles_starter_returns3() {
        assertThat(service.getMaxOpenCaseFiles("STARTER")).isEqualTo(3);
    }

    @Test
    void getMaxOpenCaseFiles_pro_returns20() {
        assertThat(service.getMaxOpenCaseFiles("PRO")).isEqualTo(20);
    }

    @Test
    void getMaxOpenCaseFiles_unknown_returnsStarterDefault() {
        assertThat(service.getMaxOpenCaseFiles("UNKNOWN")).isEqualTo(3);
    }

    // --- isExpiredFree ---

    @Test
    void isExpiredFree_freeExpired_returnsTrue() {
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        assertThat(service.isExpiredFree(sub)).isTrue();
    }

    @Test
    void isExpiredFree_freeNotExpired_returnsFalse() {
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        assertThat(service.isExpiredFree(sub)).isFalse();
    }

    @Test
    void isExpiredFree_starterWithPastExpiresAt_returnsFalse() {
        Subscription sub = new Subscription();
        sub.setPlanCode("STARTER");
        sub.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        assertThat(service.isExpiredFree(sub)).isFalse();
    }

    // --- getMaxOpenCaseFilesForWorkspace avec expiration ---

    @Test
    void getMaxOpenCaseFilesForWorkspace_freeExpired_returns0() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThat(service.getMaxOpenCaseFilesForWorkspace(workspaceId)).isEqualTo(0);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_freeActive_returns1() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThat(service.getMaxOpenCaseFilesForWorkspace(workspaceId)).isEqualTo(1);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_withStarterSubscription_returns3() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("STARTER");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThat(service.getMaxOpenCaseFilesForWorkspace(workspaceId)).isEqualTo(3);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_withProSubscription_returns20() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThat(service.getMaxOpenCaseFilesForWorkspace(workspaceId)).isEqualTo(20);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_noSubscription_returnsMaxValue() {
        UUID workspaceId = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

        assertThat(service.getMaxOpenCaseFilesForWorkspace(workspaceId)).isEqualTo(Integer.MAX_VALUE);
    }

    // --- getMaxDocumentsPerCaseFileForWorkspace avec expiration ---

    @Test
    void getMaxDocumentsPerCaseFileForWorkspace_freeExpired_returns0() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThat(service.getMaxDocumentsPerCaseFileForWorkspace(workspaceId)).isEqualTo(0);
    }

    // --- isEnrichedAnalysisAllowedForWorkspace avec expiration ---

    @Test
    void isEnrichedAnalysisAllowedForWorkspace_freeExpired_returnsFalse() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThat(service.isEnrichedAnalysisAllowedForWorkspace(workspaceId)).isFalse();
    }

    // --- isReAnalysisLimitReached ---

    // U-03 : PRO, sous la limite → false
    @Test
    void isReAnalysisLimitReached_proUnderLimit_returnsFalse() {
        UUID workspaceId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(caseFileId, fr.ailegalcase.analysis.JobType.ENRICHED_ANALYSIS))
                .thenReturn(4L);

        assertThat(service.isReAnalysisLimitReached(caseFileId, workspaceId)).isFalse();
    }

    // U-04 : PRO, à la limite → true
    @Test
    void isReAnalysisLimitReached_proAtLimit_returnsTrue() {
        UUID workspaceId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(caseFileId, fr.ailegalcase.analysis.JobType.ENRICHED_ANALYSIS))
                .thenReturn(5L);

        assertThat(service.isReAnalysisLimitReached(caseFileId, workspaceId)).isTrue();
    }

    // U-05 : sans subscription → false (fail open)
    @Test
    void isReAnalysisLimitReached_noSubscription_returnsFalse() {
        UUID workspaceId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

        assertThat(service.isReAnalysisLimitReached(caseFileId, workspaceId)).isFalse();
    }
}
