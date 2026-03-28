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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanLimitServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private UsageEventRepository usageEventRepository;
    @Mock private ChatMessageRepository chatMessageRepository;

    private PlanLimitService service;

    @BeforeEach
    void setUp() {
        service = new PlanLimitService(subscriptionRepository, usageEventRepository, chatMessageRepository);
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
}
