package fr.ailegalcase.superadmin;

import java.util.List;

public record PipelineHealthResponse(
        List<QueueHealth> queues,
        JobCounts jobsLast24h,
        JobCounts jobsLast7d,
        boolean rabbitmqAvailable) {}
