package fr.ailegalcase.analysis;

public record AnalysisJobResponse(
        String jobType,
        String status,
        int totalItems,
        int processedItems,
        int progressPercentage
) {
    static AnalysisJobResponse from(AnalysisJob job) {
        int percentage = job.getTotalItems() == 0 ? 0
                : (int) Math.floor((double) job.getProcessedItems() * 100 / job.getTotalItems());
        return new AnalysisJobResponse(
                job.getJobType().name(),
                job.getStatus().name(),
                job.getTotalItems(),
                job.getProcessedItems(),
                percentage
        );
    }
}
