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
}
