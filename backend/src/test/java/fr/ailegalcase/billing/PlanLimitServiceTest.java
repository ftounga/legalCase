package fr.ailegalcase.billing;

import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.chat.ChatMessageRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanLimitServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UsageEventRepository usageEventRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    private PlanLimitService service;

    @BeforeEach
    void setUp() {
        service = new PlanLimitService(subscriptionRepository, usageEventRepository, chatMessageRepository);
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

    // --- isMonthlyTokenBudgetExceeded ---

    // U-01 : FREE sous budget → false
    @Test
    void isMonthlyTokenBudgetExceeded_freeUnderBudget_returnsFalse() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(workspaceId), any(Instant.class)))
                .thenReturn(499_999L);

        assertThat(service.isMonthlyTokenBudgetExceeded(workspaceId)).isFalse();
    }

    // U-02 : FREE au-dessus du budget → true
    @Test
    void isMonthlyTokenBudgetExceeded_freeOverBudget_returnsTrue() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(workspaceId), any(Instant.class)))
                .thenReturn(500_000L);

        assertThat(service.isMonthlyTokenBudgetExceeded(workspaceId)).isTrue();
    }

    // U-03 : PRO sous budget → false
    @Test
    void isMonthlyTokenBudgetExceeded_proUnderBudget_returnsFalse() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(workspaceId), any(Instant.class)))
                .thenReturn(19_999_999L);

        assertThat(service.isMonthlyTokenBudgetExceeded(workspaceId)).isFalse();
    }

    // U-04 : PRO au-dessus du budget → true
    @Test
    void isMonthlyTokenBudgetExceeded_proOverBudget_returnsTrue() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(workspaceId), any(Instant.class)))
                .thenReturn(20_000_000L);

        assertThat(service.isMonthlyTokenBudgetExceeded(workspaceId)).isTrue();
    }

    // U-05 : sans subscription → false (fail open)
    @Test
    void isMonthlyTokenBudgetExceeded_noSubscription_returnsFalse() {
        UUID workspaceId = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

        assertThat(service.isMonthlyTokenBudgetExceeded(workspaceId)).isFalse();
    }

    // --- getMonthlyTokenBudgetForWorkspace ---

    // U-06 (SF-34-02) : PRO → 20 000 000
    @Test
    void getMonthlyTokenBudgetForWorkspace_pro_returns20M() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));

        assertThat(service.getMonthlyTokenBudgetForWorkspace(workspaceId)).isEqualTo(20_000_000L);
    }

    // U-07 (SF-34-02) : sans souscription → 0 (illimité)
    @Test
    void getMonthlyTokenBudgetForWorkspace_noSubscription_returns0() {
        UUID workspaceId = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

        assertThat(service.getMonthlyTokenBudgetForWorkspace(workspaceId)).isEqualTo(0L);
    }

    // --- isChatMessageLimitReached ---

    // U-08 (SF-35-01) : PRO, sous la limite → false
    @Test
    void isChatMessageLimitReached_proUnderLimit_returnsFalse() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
        when(chatMessageRepository.countByWorkspaceIdSince(eq(workspaceId), any(Instant.class)))
                .thenReturn(199L);

        assertThat(service.isChatMessageLimitReached(workspaceId)).isFalse();
    }

    // U-09 (SF-35-01) : FREE, à la limite → true
    @Test
    void isChatMessageLimitReached_freeAtLimit_returnsTrue() {
        UUID workspaceId = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
        when(chatMessageRepository.countByWorkspaceIdSince(eq(workspaceId), any(Instant.class)))
                .thenReturn(10L);

        assertThat(service.isChatMessageLimitReached(workspaceId)).isTrue();
    }

    // U-10 (SF-35-01) : sans souscription → false (fail open)
    @Test
    void isChatMessageLimitReached_noSubscription_returnsFalse() {
        UUID workspaceId = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.empty());

        assertThat(service.isChatMessageLimitReached(workspaceId)).isFalse();
    }
}
