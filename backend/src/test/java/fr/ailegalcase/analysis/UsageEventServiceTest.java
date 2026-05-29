package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UsageEventServiceTest {

    private final UsageEventRepository repository = mock(UsageEventRepository.class);
    private final UsageEventService service = new UsageEventService(repository, 0.000003, 0.000015);

    // U-01 : record nominal → event persisté avec coût calculé
    @Test
    void record_nominal_persistsEventWithCorrectCost() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        service.record(caseFileId, userId, JobType.CASE_ANALYSIS, 1000, 200);

        ArgumentCaptor<UsageEvent> captor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(repository).save(captor.capture());
        UsageEvent saved = captor.getValue();

        assertThat(saved.getCaseFileId()).isEqualTo(caseFileId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getEventType()).isEqualTo(JobType.CASE_ANALYSIS);
        assertThat(saved.getTokensInput()).isEqualTo(1000);
        assertThat(saved.getTokensOutput()).isEqualTo(200);
        // 1000 * 0.000003 + 200 * 0.000015 = 0.003 + 0.003 = 0.006000
        assertThat(saved.getEstimatedCost()).isEqualByComparingTo(new BigDecimal("0.006000"));
    }

    // U-02 : zéro tokens → coût = 0
    @Test
    void record_zeroTokens_costIsZero() {
        service.record(UUID.randomUUID(), UUID.randomUUID(), JobType.CHUNK_ANALYSIS, 0, 0);

        ArgumentCaptor<UsageEvent> captor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEstimatedCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // U-03 : event_type ENRICHED_ANALYSIS → persisté correctement
    @Test
    void record_enrichedAnalysis_eventTypePersisted() {
        service.record(UUID.randomUUID(), UUID.randomUUID(), JobType.ENRICHED_ANALYSIS, 500, 100);

        ArgumentCaptor<UsageEvent> captor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(JobType.ENRICHED_ANALYSIS);
    }

    // U-04 (SF-257-02) : job SYSTEM_* sans dossier ni utilisateur → caseFileId et
    // userId null acceptés, aucune exception (cf. régression bootstrap F-JU-01).
    @Test
    void record_systemJob_acceptsNullCaseFileAndUser() {
        service.record(null, null, JobType.SYSTEM_JP_BOOTSTRAP, 4000, 150);

        ArgumentCaptor<UsageEvent> captor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(repository).save(captor.capture());
        UsageEvent saved = captor.getValue();

        assertThat(saved.getCaseFileId()).isNull();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getEventType()).isEqualTo(JobType.SYSTEM_JP_BOOTSTRAP);
        assertThat(saved.getTokensInput()).isEqualTo(4000);
    }

    // U-05 (SF-257-02) : event_type le plus long (SYSTEM_JURISPRUDENCE_VERIFICATION,
    // 33 car.) → persisté sans troncature (colonne élargie à varchar(40)).
    @Test
    void record_longestSystemEventType_persistedWithoutTruncation() {
        assertThat(JobType.SYSTEM_JURISPRUDENCE_VERIFICATION.name().length()).isLessThanOrEqualTo(40);

        service.record(null, null, JobType.SYSTEM_JURISPRUDENCE_VERIFICATION, 100, 10);

        ArgumentCaptor<UsageEvent> captor = ArgumentCaptor.forClass(UsageEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(JobType.SYSTEM_JURISPRUDENCE_VERIFICATION);
    }
}
