package fr.ailegalcase.superadmin;

import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.RabbitMQConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class PipelineHealthService {

    private static final List<String> MONITORED_QUEUES = List.of(
            RabbitMQConfig.CHUNK_ANALYSIS_QUEUE,
            RabbitMQConfig.DOCUMENT_ANALYSIS_QUEUE,
            RabbitMQConfig.CASE_ANALYSIS_QUEUE
    );

    private final RabbitMQManagementClient managementClient;
    private final AnalysisJobRepository analysisJobRepository;

    public PipelineHealthService(RabbitMQManagementClient managementClient,
                                 AnalysisJobRepository analysisJobRepository) {
        this.managementClient = managementClient;
        this.analysisJobRepository = analysisJobRepository;
    }

    @Transactional(readOnly = true)
    public PipelineHealthResponse getHealth() {
        List<QueueHealth> queues = MONITORED_QUEUES.stream()
                .map(this::fetchQueueHealth)
                .toList();

        boolean rabbitmqAvailable = queues.stream().anyMatch(QueueHealth::available);

        Instant now = Instant.now();
        JobCounts last24h = countJobs(now.minus(24, ChronoUnit.HOURS));
        JobCounts last7d = countJobs(now.minus(7, ChronoUnit.DAYS));

        return new PipelineHealthResponse(queues, last24h, last7d, rabbitmqAvailable);
    }

    private QueueHealth fetchQueueHealth(String queueName) {
        Optional<RabbitMQManagementClient.QueueStats> stats = managementClient.getQueue(queueName);
        return stats.map(s -> new QueueHealth(queueName, s.messagesReady(),
                        s.messagesUnacknowledged(), s.consumers(), true))
                .orElse(new QueueHealth(queueName, -1, -1, -1, false));
    }

    private JobCounts countJobs(Instant since) {
        long done = analysisJobRepository.countByStatusAndUpdatedAtAfter(AnalysisStatus.DONE, since);
        long failed = analysisJobRepository.countByStatusAndUpdatedAtAfter(AnalysisStatus.FAILED, since);
        long processing = analysisJobRepository.countByStatusAndUpdatedAtAfter(AnalysisStatus.PROCESSING, since);
        long pending = analysisJobRepository.countByStatusAndUpdatedAtAfter(AnalysisStatus.PENDING, since);
        return new JobCounts(done, failed, processing, pending);
    }
}
