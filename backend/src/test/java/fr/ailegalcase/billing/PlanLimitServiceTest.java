package fr.ailegalcase.billing;

import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.chat.ChatMessageRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PlanLimitServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private UsageEventRepository usageEventRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private CreditPurchaseService creditPurchaseService;
    @Mock private WorkspaceRepository workspaceRepository;

    private PlanLimitService service;

    @BeforeEach
    void setUp() {
        service = new PlanLimitService(subscriptionRepository, usageEventRepository,
                chatMessageRepository, creditPurchaseService, workspaceRepository);
        // Default: no credits purchased (lenient — not all tests use isMonthlyTokenBudgetExceeded)
        lenient().when(creditPurchaseService.getTotalTokensBought(any())).thenReturn(0L);
    }

    // ── getMaxOpenCaseFiles ───────────────────────────────────────────────

    @Test void getMaxOpenCaseFiles_free_returns2()   { assertThat(service.getMaxOpenCaseFiles("FREE")).isEqualTo(2); }
    @Test void getMaxOpenCaseFiles_solo_returns15()  { assertThat(service.getMaxOpenCaseFiles("SOLO")).isEqualTo(15); }
    @Test void getMaxOpenCaseFiles_team_returns40()  { assertThat(service.getMaxOpenCaseFiles("TEAM")).isEqualTo(40); }
    @Test void getMaxOpenCaseFiles_pro_returnsMax()  { assertThat(service.getMaxOpenCaseFiles("PRO")).isEqualTo(Integer.MAX_VALUE); }
    @Test void getMaxOpenCaseFiles_unknown_returnsFreeDefault() { assertThat(service.getMaxOpenCaseFiles("UNKNOWN")).isEqualTo(2); }

    // ── getMaxDocumentsPerCaseFile ────────────────────────────────────────

    @Test void getMaxDocumentsPerCaseFile_free_returns5()  { assertThat(service.getMaxDocumentsPerCaseFile("FREE")).isEqualTo(5); }
    @Test void getMaxDocumentsPerCaseFile_solo_returns15() { assertThat(service.getMaxDocumentsPerCaseFile("SOLO")).isEqualTo(15); }
    @Test void getMaxDocumentsPerCaseFile_team_returns30() { assertThat(service.getMaxDocumentsPerCaseFile("TEAM")).isEqualTo(30); }
    @Test void getMaxDocumentsPerCaseFile_pro_returns50()  { assertThat(service.getMaxDocumentsPerCaseFile("PRO")).isEqualTo(50); }

    // ── isExpiredFree ─────────────────────────────────────────────────────

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
    void isExpiredFree_soloWithPastExpiresAt_returnsFalse() {
        Subscription sub = new Subscription();
        sub.setPlanCode("SOLO");
        sub.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        assertThat(service.isExpiredFree(sub)).isFalse();
    }

    // ── getMaxOpenCaseFilesForWorkspace ───────────────────────────────────

    @Test
    void getMaxOpenCaseFilesForWorkspace_freeExpired_returns0() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.getMaxOpenCaseFilesForWorkspace(wid)).isEqualTo(0);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_freeActive_returns2() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.getMaxOpenCaseFilesForWorkspace(wid)).isEqualTo(2);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_solo_returns15() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.getMaxOpenCaseFilesForWorkspace(wid)).isEqualTo(15);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_team_returns40() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("TEAM");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.getMaxOpenCaseFilesForWorkspace(wid)).isEqualTo(40);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_pro_returnsMaxValue() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.getMaxOpenCaseFilesForWorkspace(wid)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void getMaxOpenCaseFilesForWorkspace_noSubscription_returnsMaxValue() {
        UUID wid = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.empty());
        assertThat(service.getMaxOpenCaseFilesForWorkspace(wid)).isEqualTo(Integer.MAX_VALUE);
    }

    // ── getMaxDocumentsPerCaseFileForWorkspace ────────────────────────────

    @Test
    void getMaxDocumentsPerCaseFileForWorkspace_freeExpired_returns0() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.getMaxDocumentsPerCaseFileForWorkspace(wid)).isEqualTo(0);
    }

    // ── isEnrichedAnalysisAllowedForWorkspace ─────────────────────────────

    @Test
    void isEnrichedAnalysisAllowedForWorkspace_freeExpired_returnsFalse() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.isEnrichedAnalysisAllowedForWorkspace(wid)).isFalse();
    }

    @Test
    void isEnrichedAnalysisAllowedForWorkspace_freeActive_returnsFalse() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.isEnrichedAnalysisAllowedForWorkspace(wid)).isFalse();
    }

    @Test
    void isEnrichedAnalysisAllowedForWorkspace_solo_returnsTrue() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.isEnrichedAnalysisAllowedForWorkspace(wid)).isTrue();
    }

    @Test
    void isEnrichedAnalysisAllowedForWorkspace_team_returnsTrue() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("TEAM");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.isEnrichedAnalysisAllowedForWorkspace(wid)).isTrue();
    }

    @Test
    void isEnrichedAnalysisAllowedForWorkspace_pro_returnsTrue() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.isEnrichedAnalysisAllowedForWorkspace(wid)).isTrue();
    }

    // ── isReAnalysisLimitReached ──────────────────────────────────────────

    @Test
    void isReAnalysisLimitReached_free_underLimit_returnsFalse() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.ENRICHED_ANALYSIS)).thenReturn(0L);
        assertThat(service.isReAnalysisLimitReached(cfid, wid)).isFalse();
    }

    @Test
    void isReAnalysisLimitReached_free_atLimit_returnsTrue() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.ENRICHED_ANALYSIS)).thenReturn(1L);
        assertThat(service.isReAnalysisLimitReached(cfid, wid)).isTrue();
    }

    @Test
    void isReAnalysisLimitReached_solo_underLimit_returnsFalse() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.ENRICHED_ANALYSIS)).thenReturn(2L);
        assertThat(service.isReAnalysisLimitReached(cfid, wid)).isFalse();
    }

    @Test
    void isReAnalysisLimitReached_solo_atLimit_returnsTrue() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.ENRICHED_ANALYSIS)).thenReturn(3L);
        assertThat(service.isReAnalysisLimitReached(cfid, wid)).isTrue();
    }

    @Test
    void isReAnalysisLimitReached_team_underLimit_returnsFalse() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("TEAM");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.ENRICHED_ANALYSIS)).thenReturn(7L);
        assertThat(service.isReAnalysisLimitReached(cfid, wid)).isFalse();
    }

    @Test
    void isReAnalysisLimitReached_team_atLimit_returnsTrue() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("TEAM");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.ENRICHED_ANALYSIS)).thenReturn(8L);
        assertThat(service.isReAnalysisLimitReached(cfid, wid)).isTrue();
    }

    @Test
    void isReAnalysisLimitReached_pro_alwaysReturnsFalse() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.isReAnalysisLimitReached(cfid, wid)).isFalse();
        verify(usageEventRepository, never()).countByCaseFileIdAndEventType(any(), any());
    }

    @Test
    void isReAnalysisLimitReached_expiredFree_returnsTrue() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().minusSeconds(3600));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.isReAnalysisLimitReached(cfid, wid)).isTrue();
        verify(usageEventRepository, never()).countByCaseFileIdAndEventType(any(), any());
    }

    @Test
    void isReAnalysisLimitReached_noSubscription_returnsFalse() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.empty());
        assertThat(service.isReAnalysisLimitReached(cfid, wid)).isFalse();
    }

    // ── isMonthlyTokenBudgetExceeded ──────────────────────────────────────

    @Test
    void isMonthlyTokenBudgetExceeded_freeUnderBudget_returnsFalse() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(499_999L);
        assertThat(service.isMonthlyTokenBudgetExceeded(wid)).isFalse();
    }

    @Test
    void isMonthlyTokenBudgetExceeded_freeAtBudget_returnsTrue() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(500_000L);
        assertThat(service.isMonthlyTokenBudgetExceeded(wid)).isTrue();
    }

    @Test
    void isMonthlyTokenBudgetExceeded_soloUnderBudget_returnsFalse() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(5_999_999L);
        assertThat(service.isMonthlyTokenBudgetExceeded(wid)).isFalse();
    }

    @Test
    void isMonthlyTokenBudgetExceeded_proUnderBudget_returnsFalse() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(59_999_999L);
        assertThat(service.isMonthlyTokenBudgetExceeded(wid)).isFalse();
    }

    @Test
    void isMonthlyTokenBudgetExceeded_proAtBudget_returnsTrue() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(60_000_000L);
        assertThat(service.isMonthlyTokenBudgetExceeded(wid)).isTrue();
    }

    @Test
    void isMonthlyTokenBudgetExceeded_noSubscription_returnsFalse() {
        UUID wid = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.empty());
        assertThat(service.isMonthlyTokenBudgetExceeded(wid)).isFalse();
    }

    // ── getMonthlyTokenBudgetForWorkspace ─────────────────────────────────

    @Test
    void getMonthlyTokenBudgetForWorkspace_free_returns500k()  {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.getMonthlyTokenBudgetForWorkspace(wid)).isEqualTo(500_000L);
    }

    @Test
    void getMonthlyTokenBudgetForWorkspace_solo_returns6M() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.getMonthlyTokenBudgetForWorkspace(wid)).isEqualTo(6_000_000L);
    }

    @Test
    void getMonthlyTokenBudgetForWorkspace_team_returns18M() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("TEAM");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.getMonthlyTokenBudgetForWorkspace(wid)).isEqualTo(18_000_000L);
    }

    @Test
    void getMonthlyTokenBudgetForWorkspace_pro_returns60M() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.getMonthlyTokenBudgetForWorkspace(wid)).isEqualTo(60_000_000L);
    }

    @Test
    void getMonthlyTokenBudgetForWorkspace_noSubscription_returns0() {
        UUID wid = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.empty());
        assertThat(service.getMonthlyTokenBudgetForWorkspace(wid)).isEqualTo(0L);
    }

    @Test
    void getMonthlyTokenBudgetForWorkspace_withUnusedCredits_returnsPlanBudgetPlusCredits() {
        // SOLO plan (6M/month, monthsActive=1), 1M de crédits achetés, 0 consommé → budget = 7M
        // startedAt=null → fallback Instant.now() → months_between=0 → monthsActive=1
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(creditPurchaseService.getTotalTokensBought(wid)).thenReturn(1_000_000L);
        when(usageEventRepository.sumTokensByWorkspaceIdAllTime(wid)).thenReturn(0L);

        assertThat(service.getMonthlyTokenBudgetForWorkspace(wid)).isEqualTo(7_000_000L);
    }

    @Test
    void getMonthlyTokenBudgetForWorkspace_withPartiallyConsumedCredits_returnsReducedBonus() {
        // SOLO (6M/month, monthsActive=1), 2M crédits, 7M consommés all-time
        // creditsConsumed = max(0, 7M - 6M) = 1M → creditsRemaining = max(0, 2M - 1M) = 1M
        // budget = 6M + 1M = 7M
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(creditPurchaseService.getTotalTokensBought(wid)).thenReturn(2_000_000L);
        when(usageEventRepository.sumTokensByWorkspaceIdAllTime(wid)).thenReturn(7_000_000L);

        assertThat(service.getMonthlyTokenBudgetForWorkspace(wid)).isEqualTo(7_000_000L);
    }

    @Test
    void getMonthlyTokenBudgetForWorkspace_creditsFullyConsumed_returnsPlanBudgetOnly() {
        // SOLO (6M/month, monthsActive=1), 1M crédits, 8M consommés all-time → credits épuisés
        // creditsConsumed = max(0, 8M - 6M) = 2M > 1M achetés → creditsRemaining = 0
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(creditPurchaseService.getTotalTokensBought(wid)).thenReturn(1_000_000L);
        when(usageEventRepository.sumTokensByWorkspaceIdAllTime(wid)).thenReturn(8_000_000L);

        assertThat(service.getMonthlyTokenBudgetForWorkspace(wid)).isEqualTo(6_000_000L);
    }

    // ── isChatMessageLimitReached ─────────────────────────────────────────

    @Test
    void isChatMessageLimitReached_soloUnderLimit_returnsFalse() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(chatMessageRepository.countByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(99L);
        assertThat(service.isChatMessageLimitReached(wid)).isFalse();
    }

    @Test
    void isChatMessageLimitReached_soloAtLimit_returnsTrue() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(chatMessageRepository.countByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(100L);
        assertThat(service.isChatMessageLimitReached(wid)).isTrue();
    }

    @Test
    void isChatMessageLimitReached_proUnderLimit_returnsFalse() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(chatMessageRepository.countByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(999L);
        assertThat(service.isChatMessageLimitReached(wid)).isFalse();
    }

    @Test
    void isChatMessageLimitReached_freeAtLimit_returnsTrue() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(chatMessageRepository.countByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(10L);
        assertThat(service.isChatMessageLimitReached(wid)).isTrue();
    }

    @Test
    void isChatMessageLimitReached_noSubscription_returnsFalse() {
        UUID wid = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.empty());
        assertThat(service.isChatMessageLimitReached(wid)).isFalse();
    }

    // ── isCaseAnalysisLimitReached ────────────────────────────────────────

    @Test
    void isCaseAnalysisLimitReached_freeUnderLimit_returnsFalse() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.CASE_ANALYSIS)).thenReturn(1L);
        assertThat(service.isCaseAnalysisLimitReached(cfid, wid)).isFalse();
    }

    @Test
    void isCaseAnalysisLimitReached_freeAtLimit_returnsTrue() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("FREE");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.CASE_ANALYSIS)).thenReturn(2L);
        assertThat(service.isCaseAnalysisLimitReached(cfid, wid)).isTrue();
    }

    @Test
    void isCaseAnalysisLimitReached_soloUnderLimit_returnsFalse() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.CASE_ANALYSIS)).thenReturn(7L);
        assertThat(service.isCaseAnalysisLimitReached(cfid, wid)).isFalse();
    }

    @Test
    void isCaseAnalysisLimitReached_soloAtLimit_returnsTrue() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.CASE_ANALYSIS)).thenReturn(8L);
        assertThat(service.isCaseAnalysisLimitReached(cfid, wid)).isTrue();
    }

    @Test
    void isCaseAnalysisLimitReached_teamUnderLimit_returnsFalse() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("TEAM");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.CASE_ANALYSIS)).thenReturn(14L);
        assertThat(service.isCaseAnalysisLimitReached(cfid, wid)).isFalse();
    }

    @Test
    void isCaseAnalysisLimitReached_teamAtLimit_returnsTrue() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("TEAM");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.countByCaseFileIdAndEventType(cfid, JobType.CASE_ANALYSIS)).thenReturn(15L);
        assertThat(service.isCaseAnalysisLimitReached(cfid, wid)).isTrue();
    }

    @Test
    void isCaseAnalysisLimitReached_pro_alwaysReturnsFalse() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("PRO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.isCaseAnalysisLimitReached(cfid, wid)).isFalse();
        verify(usageEventRepository, never()).countByCaseFileIdAndEventType(any(), any());
    }

    @Test
    void isCaseAnalysisLimitReached_expiredFree_returnsTrue() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setExpiresAt(Instant.now().minusSeconds(3600));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        assertThat(service.isCaseAnalysisLimitReached(cfid, wid)).isTrue();
        verify(usageEventRepository, never()).countByCaseFileIdAndEventType(any(), any());
    }

    @Test
    void isCaseAnalysisLimitReached_noSubscription_returnsFalse() {
        UUID wid = UUID.randomUUID(); UUID cfid = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.empty());
        assertThat(service.isCaseAnalysisLimitReached(cfid, wid)).isFalse();
    }

    // ── isMonthlyTokenBudgetExceeded avec crédits ─────────────────────────

    // CR-01 : budget dépassé MAIS crédits couvrent le dépassement → false
    @Test
    void isMonthlyTokenBudgetExceeded_withCredits_coversOverage_returnsFalse() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("SOLO"); // budget = 6M
        sub.setStartedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        // This month: 6.5M used (over by 500K)
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(6_500_000L);
        // All time: 6.5M (same, first month)
        when(usageEventRepository.sumTokensByWorkspaceIdAllTime(wid)).thenReturn(6_500_000L);
        // Credits: 1M bought
        when(creditPurchaseService.getTotalTokensBought(wid)).thenReturn(1_000_000L);

        assertThat(service.isMonthlyTokenBudgetExceeded(wid)).isFalse();
    }

    // CR-02 : budget dépassé et crédits insuffisants → true
    @Test
    void isMonthlyTokenBudgetExceeded_withCredits_notEnough_returnsTrue() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("SOLO"); // budget = 6M
        sub.setStartedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        // This month: 7.5M used (over by 1.5M)
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(7_500_000L);
        when(usageEventRepository.sumTokensByWorkspaceIdAllTime(wid)).thenReturn(7_500_000L);
        // Credits: 1M bought (covers only 1M of 1.5M overage)
        when(creditPurchaseService.getTotalTokensBought(wid)).thenReturn(1_000_000L);

        assertThat(service.isMonthlyTokenBudgetExceeded(wid)).isTrue();
    }

    // CR-03 : crédits entièrement consommés sur des mois précédents → traités comme épuisés
    @Test
    void isMonthlyTokenBudgetExceeded_creditsAlreadyConsumedInPriorMonths_returnsTrue() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription();
        sub.setPlanCode("SOLO"); // budget = 6M/mois
        // 35 jours = exactement 1 mois complet → monthsActive = 2
        sub.setStartedAt(Instant.now().minus(35, ChronoUnit.DAYS));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        // This month: 6.5M used
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(6_500_000L);
        // All time: 13.5M (2 mois × 6M plan + 1.5M crédit déjà consommé)
        when(usageEventRepository.sumTokensByWorkspaceIdAllTime(wid)).thenReturn(13_500_000L);
        // Credits bought: 1M (mais déjà consommés le mois dernier)
        when(creditPurchaseService.getTotalTokensBought(wid)).thenReturn(1_000_000L);
        // allTimePlanBudget = 2 months × 6M = 12M
        // creditsConsumed = max(0, 13.5M - 12M) = 1.5M → > 1M bought → creditsRemaining = 0
        // exceeded = 6.5M >= 6M + 0 = true

        assertThat(service.isMonthlyTokenBudgetExceeded(wid)).isTrue();
    }

    // CR-04 : workspace sans crédits → comportement identique à avant
    @Test
    void isMonthlyTokenBudgetExceeded_noCredits_behavesAsBeforeF49() {
        UUID wid = UUID.randomUUID();
        Subscription sub = new Subscription(); sub.setPlanCode("SOLO");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(usageEventRepository.sumTokensByWorkspaceIdSince(eq(wid), any(Instant.class))).thenReturn(6_000_000L);
        // getTotalTokensBought returns 0L by default (setUp)

        assertThat(service.isMonthlyTokenBudgetExceeded(wid)).isTrue();
    }

    // ── SF-122-02 : Quotas OCR ───────────────────────────────────────────

    @Test void U_PLS_OCR_01_getMonthlyOcrPages_FREE()  { assertThat(service.getMonthlyOcrPages("FREE")).isEqualTo(100); }
    @Test void U_PLS_OCR_02_getMonthlyOcrPages_SOLO()  { assertThat(service.getMonthlyOcrPages("SOLO")).isEqualTo(800); }
    @Test void U_PLS_OCR_03_getMonthlyOcrPages_TEAM()  { assertThat(service.getMonthlyOcrPages("TEAM")).isEqualTo(3_000); }
    @Test void U_PLS_OCR_04_getMonthlyOcrPages_PRO()   { assertThat(service.getMonthlyOcrPages("PRO")).isEqualTo(10_000); }
    @Test void U_PLS_OCR_unknown_defaultsToFree()     { assertThat(service.getMonthlyOcrPages("UNKNOWN")).isEqualTo(100); }

    // U-PLS-OCR-05 : SOLO current_month=800, ajout 5 → 805 > 800 → exceeded
    @Test
    void U_PLS_OCR_05_monthlyLimitExceeded() {
        UUID wid = UUID.randomUUID();
        Workspace ws = workspaceWithOcrUsage(wid, 800, 0, LocalDate.now());
        Subscription sub = subscriptionWithPlan("SOLO");
        when(workspaceRepository.findById(wid)).thenReturn(Optional.of(ws));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));

        assertThat(service.isOcrQuotaExceeded(wid, 5)).isTrue();
    }

    // U-PLS-OCR-06 : SOLO current_month=790, ajout 5 → 795 ≤ 800 → OK
    @Test
    void U_PLS_OCR_06_monthlyWithinLimit() {
        UUID wid = UUID.randomUUID();
        Workspace ws = workspaceWithOcrUsage(wid, 790, 0, LocalDate.now());
        when(workspaceRepository.findById(wid)).thenReturn(Optional.of(ws));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(subscriptionWithPlan("SOLO")));

        assertThat(service.isOcrQuotaExceeded(wid, 5)).isFalse();
    }

    // U-PLS-OCR-07 : compteur stale (lastReset mois passé) traité comme 0 → pas de dépassement
    @Test
    void U_PLS_OCR_07_staleMonthlyCounter_treatedAsZero() {
        UUID wid = UUID.randomUUID();
        LocalDate pastMonth = LocalDate.now().minusMonths(2);
        Workspace ws = workspaceWithOcrUsage(wid, 800, 800, pastMonth);
        when(workspaceRepository.findById(wid)).thenReturn(Optional.of(ws));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(subscriptionWithPlan("SOLO")));

        assertThat(service.isOcrQuotaExceeded(wid, 50)).isFalse();
    }

    // U-PLS-OCR-08 : hard cap journalier — 450 aujourd'hui + 60 = 510 > 500 → exceeded
    @Test
    void U_PLS_OCR_08_dailyHardCapExceeded() {
        UUID wid = UUID.randomUUID();
        Workspace ws = workspaceWithOcrUsage(wid, 450, 450, LocalDate.now());
        when(workspaceRepository.findById(wid)).thenReturn(Optional.of(ws));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(subscriptionWithPlan("TEAM")));

        assertThat(service.isOcrQuotaExceeded(wid, 60)).isTrue();
    }

    // U-PLS-OCR-09 : compteur journalier stale (lastReset hier) → current_day effectif = 0
    @Test
    void U_PLS_OCR_09_staleDailyCounter_treatedAsZero() {
        UUID wid = UUID.randomUUID();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Workspace ws = workspaceWithOcrUsage(wid, 50, 500, yesterday);
        when(workspaceRepository.findById(wid)).thenReturn(Optional.of(ws));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(subscriptionWithPlan("TEAM")));

        assertThat(service.isOcrQuotaExceeded(wid, 100)).isFalse();
    }

    // U-PLS-OCR-10 : workspace introuvable → blocked (défaut sécurité)
    @Test
    void U_PLS_OCR_10_workspaceNotFound_blocks() {
        UUID wid = UUID.randomUUID();
        when(workspaceRepository.findById(wid)).thenReturn(Optional.empty());

        assertThat(service.isOcrQuotaExceeded(wid, 5)).isTrue();
    }

    // U-PLS-OCR-11 : additionalPages = 0 → never exceeded (edge case safeguard)
    @Test
    void U_PLS_OCR_11_zeroPages_neverExceeded() {
        UUID wid = UUID.randomUUID();
        assertThat(service.isOcrQuotaExceeded(wid, 0)).isFalse();
        verify(workspaceRepository, never()).findById(any());
    }

    private Workspace workspaceWithOcrUsage(UUID id, int monthlyPages, int dailyPages, LocalDate lastReset) {
        Workspace ws = new Workspace();
        ws.setId(id);
        ws.setOcrPagesUsedCurrentMonth(monthlyPages);
        ws.setOcrPagesUsedCurrentDay(dailyPages);
        ws.setOcrUsageLastResetDate(lastReset);
        return ws;
    }

    private Subscription subscriptionWithPlan(String planCode) {
        Subscription sub = new Subscription();
        sub.setPlanCode(planCode);
        return sub;
    }
}
