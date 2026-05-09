package fr.ailegalcase.video;

import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.shared.PaymentRequiredCode;
import fr.ailegalcase.shared.PaymentRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SF-231-03 — Tests unitaires du service de quota vidéo. Couvre :
 * <ul>
 *   <li>conversion {@code Math.ceil(durationSeconds/60.0)} (1s→1min, 60s→1min, 61s→2min, 120s→2min) ;
 *   <li>quota OK / dépassé / partiellement consommé pour SOLO/TEAM/PRO/TRIAL ;
 *   <li>plan ENTERPRISE : illimité (jamais de throw) ;
 *   <li>insertion {@link WorkspaceVideoUsage} avec workspaceId / documentId / yearMonth / minutes corrects ;
 *   <li>edge cases : durée nulle ⇒ pas d'INSERT, workspace null ⇒ refus.
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class VideoQuotaServiceTest {

    @Mock private WorkspaceVideoUsageRepository repository;
    @Mock private PlanLimitService planLimitService;

    private VideoQuotaService service;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-09T12:00:00Z"), ZoneOffset.UTC);
    private final String currentMonth = YearMonth.now(fixedClock).toString(); // "2026-05"

    @BeforeEach
    void setUp() {
        service = new VideoQuotaService(repository, planLimitService, fixedClock);
    }

    // ── Calcul minutes (Math.ceil) ────────────────────────────────────────

    @Test void U_VQS_minutes_zero()             { assertThat(VideoQuotaService.computeMinutesFromSeconds(0)).isEqualTo(0); }
    @Test void U_VQS_minutes_negative()         { assertThat(VideoQuotaService.computeMinutesFromSeconds(-5)).isEqualTo(0); }
    @Test void U_VQS_minutes_1s()               { assertThat(VideoQuotaService.computeMinutesFromSeconds(1)).isEqualTo(1); }
    @Test void U_VQS_minutes_59s()              { assertThat(VideoQuotaService.computeMinutesFromSeconds(59)).isEqualTo(1); }
    @Test void U_VQS_minutes_60s()              { assertThat(VideoQuotaService.computeMinutesFromSeconds(60)).isEqualTo(1); }
    @Test void U_VQS_minutes_61s()              { assertThat(VideoQuotaService.computeMinutesFromSeconds(61)).isEqualTo(2); }
    @Test void U_VQS_minutes_120s()             { assertThat(VideoQuotaService.computeMinutesFromSeconds(120)).isEqualTo(2); }
    @Test void U_VQS_minutes_121s()             { assertThat(VideoQuotaService.computeMinutesFromSeconds(121)).isEqualTo(3); }

    // ── checkVideoQuotaForUpload — pré-vérification ────────────────────────

    @Test
    void U_VQS_check_solo_quotaOk_doesNotThrow() {
        UUID wid = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(5);
        when(repository.sumMinutesByWorkspaceAndMonth(wid, currentMonth)).thenReturn(2);

        // 2 min consommées + 90 s = 2 min ⇒ 4 ≤ 5 → OK
        service.checkVideoQuotaForUpload(wid, 90L);

        verify(repository, never()).save(any());
    }

    @Test
    void U_VQS_check_solo_quotaExceeded_throws402() {
        UUID wid = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(5);
        when(repository.sumMinutesByWorkspaceAndMonth(wid, currentMonth)).thenReturn(4);

        // 4 min consommées + 90 s ⇒ 4+2=6 > 5 → 402
        assertThatThrownBy(() -> service.checkVideoQuotaForUpload(wid, 90L))
                .isInstanceOf(PaymentRequiredException.class)
                .extracting(e -> ((PaymentRequiredException) e).getCode())
                .isEqualTo(PaymentRequiredCode.VIDEO_QUOTA_EXCEEDED);
    }

    @Test
    void U_VQS_check_solo_exactlyAtLimit_doesNotThrow() {
        UUID wid = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(5);
        when(repository.sumMinutesByWorkspaceAndMonth(wid, currentMonth)).thenReturn(4);

        // 4 + 30 s = 4+1=5 ≤ 5 → OK
        service.checkVideoQuotaForUpload(wid, 30L);
    }

    @Test
    void U_VQS_check_team_30min_quotaOk() {
        UUID wid = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(30);
        when(repository.sumMinutesByWorkspaceAndMonth(wid, currentMonth)).thenReturn(28);

        // 28 + 60 s = 29 ≤ 30 → OK
        service.checkVideoQuotaForUpload(wid, 60L);
    }

    @Test
    void U_VQS_check_pro_120min_quotaOk() {
        UUID wid = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(120);
        when(repository.sumMinutesByWorkspaceAndMonth(wid, currentMonth)).thenReturn(118);

        service.checkVideoQuotaForUpload(wid, 60L);
    }

    @Test
    void U_VQS_check_enterprise_unlimited_neverThrows() {
        UUID wid = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(Integer.MAX_VALUE);

        // Même 60 000 min — pas de throw, et la query sum n'est même pas appelée.
        service.checkVideoQuotaForUpload(wid, 3_600_000L);

        verify(repository, never()).sumMinutesByWorkspaceAndMonth(any(), anyString());
    }

    @Test
    void U_VQS_check_trial_1min_quotaOk_firstUpload() {
        UUID wid = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(1);
        when(repository.sumMinutesByWorkspaceAndMonth(wid, currentMonth)).thenReturn(0);

        // 0 + 1 s ⇒ 0+1=1 ≤ 1 → OK
        service.checkVideoQuotaForUpload(wid, 1L);
    }

    @Test
    void U_VQS_check_trial_1min_quotaExceededOnSecond() {
        UUID wid = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(1);
        when(repository.sumMinutesByWorkspaceAndMonth(wid, currentMonth)).thenReturn(1);

        // 1 + 1 s ⇒ 1+1=2 > 1 → 402
        assertThatThrownBy(() -> service.checkVideoQuotaForUpload(wid, 1L))
                .isInstanceOf(PaymentRequiredException.class)
                .extracting(e -> ((PaymentRequiredException) e).getCode())
                .isEqualTo(PaymentRequiredCode.VIDEO_QUOTA_EXCEEDED);
    }

    @Test
    void U_VQS_check_zeroDuration_doesNotThrow() {
        UUID wid = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(5);

        service.checkVideoQuotaForUpload(wid, 0L);

        // Aucune query sum (durée nulle ⇒ rien à consommer)
        verify(repository, never()).sumMinutesByWorkspaceAndMonth(any(), anyString());
    }

    @Test
    void U_VQS_check_nullWorkspace_throws402() {
        assertThatThrownBy(() -> service.checkVideoQuotaForUpload(null, 60L))
                .isInstanceOf(PaymentRequiredException.class);
    }

    // ── recordVideoUsage — INSERT après save() ─────────────────────────────

    @Test
    void U_VQS_record_solo_underQuota_insertsRow() {
        UUID wid = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(5);
        when(repository.sumMinutesByWorkspaceAndMonth(wid, currentMonth)).thenReturn(2);

        // 90 s ⇒ 2 min ; total 4 ≤ 5 → OK
        service.recordVideoUsage(wid, docId, 90L);

        ArgumentCaptor<WorkspaceVideoUsage> captor = ArgumentCaptor.forClass(WorkspaceVideoUsage.class);
        verify(repository).save(captor.capture());
        WorkspaceVideoUsage saved = captor.getValue();
        assertThat(saved.getWorkspaceId()).isEqualTo(wid);
        assertThat(saved.getDocumentId()).isEqualTo(docId);
        assertThat(saved.getYearMonth()).isEqualTo(currentMonth);
        assertThat(saved.getMinutesConsumed()).isEqualTo(2);
    }

    @Test
    void U_VQS_record_solo_overQuota_throwsAndDoesNotInsert() {
        UUID wid = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(5);
        when(repository.sumMinutesByWorkspaceAndMonth(wid, currentMonth)).thenReturn(4);

        // 4 + 90 s ⇒ 4+2=6 > 5 → 402
        assertThatThrownBy(() -> service.recordVideoUsage(wid, docId, 90L))
                .isInstanceOf(PaymentRequiredException.class)
                .extracting(e -> ((PaymentRequiredException) e).getCode())
                .isEqualTo(PaymentRequiredCode.VIDEO_QUOTA_EXCEEDED);

        verify(repository, never()).save(any());
    }

    @Test
    void U_VQS_record_enterprise_unlimited_skipsSumQueryAndInserts() {
        UUID wid = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(Integer.MAX_VALUE);

        // ENTERPRISE : insère sans même appeler la sum (économie I/O)
        service.recordVideoUsage(wid, docId, 1800L); // 30 min

        verify(repository, never()).sumMinutesByWorkspaceAndMonth(any(), anyString());
        ArgumentCaptor<WorkspaceVideoUsage> captor = ArgumentCaptor.forClass(WorkspaceVideoUsage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getMinutesConsumed()).isEqualTo(30);
    }

    @Test
    void U_VQS_record_zeroDuration_skipsInsert() {
        UUID wid = UUID.randomUUID();
        UUID docId = UUID.randomUUID();

        service.recordVideoUsage(wid, docId, 0L);

        verify(repository, never()).save(any());
        verify(repository, never()).sumMinutesByWorkspaceAndMonth(any(), anyString());
        verify(planLimitService, never()).getMonthlyVideoMinutesForWorkspace(any());
    }

    @Test
    void U_VQS_record_nullWorkspace_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.recordVideoUsage(null, UUID.randomUUID(), 60L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void U_VQS_record_nullDocumentId_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.recordVideoUsage(UUID.randomUUID(), null, 60L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── checkAndConsumeVideoMinutes — alias mini-spec ─────────────────────

    @Test
    void U_VQS_checkAndConsume_alias_routesToRecord() {
        UUID wid = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        when(planLimitService.getMonthlyVideoMinutesForWorkspace(wid)).thenReturn(120);
        when(repository.sumMinutesByWorkspaceAndMonth(eq(wid), anyString())).thenReturn(0);

        service.checkAndConsumeVideoMinutes(wid, 60L, docId);

        verify(repository).save(any(WorkspaceVideoUsage.class));
    }
}
