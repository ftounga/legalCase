package fr.ailegalcase.superadmin;

import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.RabbitMQConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PipelineHealthServiceTest {

    private final RabbitMQManagementClient managementClient = mock(RabbitMQManagementClient.class);
    private final AnalysisJobRepository analysisJobRepository = mock(AnalysisJobRepository.class);

    private final PipelineHealthService service =
            new PipelineHealthService(managementClient, analysisJobRepository);

    // U-01 : RabbitMQ disponible → queues available=true avec valeurs réelles
    @Test
    void getHealth_rabbitmqAvailable_returnsQueueStats() {
        when(managementClient.getQueue(RabbitMQConfig.CHUNK_ANALYSIS_QUEUE))
                .thenReturn(Optional.of(new RabbitMQManagementClient.QueueStats(12, 5, 10)));
        when(managementClient.getQueue(RabbitMQConfig.DOCUMENT_ANALYSIS_QUEUE))
                .thenReturn(Optional.of(new RabbitMQManagementClient.QueueStats(0, 2, 10)));
        when(managementClient.getQueue(RabbitMQConfig.CASE_ANALYSIS_QUEUE))
                .thenReturn(Optional.of(new RabbitMQManagementClient.QueueStats(0, 1, 6)));
        when(analysisJobRepository.countByStatusAndUpdatedAtAfter(any(), any())).thenReturn(0L);

        PipelineHealthResponse health = service.getHealth();

        assertThat(health.rabbitmqAvailable()).isTrue();
        assertThat(health.queues()).hasSize(3);

        QueueHealth chunk = health.queues().get(0);
        assertThat(chunk.name()).isEqualTo(RabbitMQConfig.CHUNK_ANALYSIS_QUEUE);
        assertThat(chunk.messagesReady()).isEqualTo(12);
        assertThat(chunk.messagesUnacknowledged()).isEqualTo(5);
        assertThat(chunk.consumers()).isEqualTo(10);
        assertThat(chunk.available()).isTrue();
    }

    // U-02 : RabbitMQ indisponible → fail-open, available=false, jobs stats DB retournés
    @Test
    void getHealth_rabbitmqUnavailable_failOpen() {
        when(managementClient.getQueue(any())).thenReturn(Optional.empty());
        when(analysisJobRepository.countByStatusAndUpdatedAtAfter(eq(AnalysisStatus.DONE), any()))
                .thenReturn(48L);
        when(analysisJobRepository.countByStatusAndUpdatedAtAfter(eq(AnalysisStatus.FAILED), any()))
                .thenReturn(0L);
        when(analysisJobRepository.countByStatusAndUpdatedAtAfter(eq(AnalysisStatus.PROCESSING), any()))
                .thenReturn(1L);
        when(analysisJobRepository.countByStatusAndUpdatedAtAfter(eq(AnalysisStatus.PENDING), any()))
                .thenReturn(0L);

        PipelineHealthResponse health = service.getHealth();

        assertThat(health.rabbitmqAvailable()).isFalse();
        assertThat(health.queues()).hasSize(3);
        health.queues().forEach(q -> {
            assertThat(q.available()).isFalse();
            assertThat(q.messagesReady()).isEqualTo(-1);
        });

        // Jobs stats DB toujours retournés malgré RabbitMQ indisponible
        assertThat(health.jobsLast24h().done()).isEqualTo(48L);
        assertThat(health.jobsLast24h().failed()).isEqualTo(0L);
        assertThat(health.jobsLast24h().processing()).isEqualTo(1L);
    }

    // U-03 : jobs stats calculés depuis analysis_jobs avec la bonne fenêtre temporelle
    @Test
    void getHealth_jobStats_queriedWithCorrectTimeWindows() {
        when(managementClient.getQueue(any())).thenReturn(Optional.empty());
        when(analysisJobRepository.countByStatusAndUpdatedAtAfter(any(), any())).thenReturn(0L);

        service.getHealth();

        // 4 statuts × 2 fenêtres (24h + 7d) = 8 appels
        verify(analysisJobRepository, times(8)).countByStatusAndUpdatedAtAfter(any(), any());

        // Vérifie que les deux fenêtres sont distinctes (24h et 7j)
        verify(analysisJobRepository, atLeastOnce()).countByStatusAndUpdatedAtAfter(
                eq(AnalysisStatus.DONE), argThat(since ->
                        since.isBefore(Instant.now().minusSeconds(23 * 3600)) &&
                        since.isAfter(Instant.now().minusSeconds(25 * 3600))));
    }

    // U-04 : les 3 queues surveillées sont bien chunk, document, case
    @Test
    void getHealth_monitorsExpectedQueues() {
        when(managementClient.getQueue(any())).thenReturn(Optional.empty());
        when(analysisJobRepository.countByStatusAndUpdatedAtAfter(any(), any())).thenReturn(0L);

        PipelineHealthResponse health = service.getHealth();

        assertThat(health.queues())
                .extracting(QueueHealth::name)
                .containsExactly(
                        RabbitMQConfig.CHUNK_ANALYSIS_QUEUE,
                        RabbitMQConfig.DOCUMENT_ANALYSIS_QUEUE,
                        RabbitMQConfig.CASE_ANALYSIS_QUEUE);
    }
}
