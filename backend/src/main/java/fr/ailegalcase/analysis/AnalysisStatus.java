package fr.ailegalcase.analysis;

public enum AnalysisStatus {
    PENDING,
    PROCESSING,
    /**
     * F-185 SF-185-01 — état SSE éphémère émis par {@code SseNotificationService}
     * quand une nouvelle section JSON top-level vient d'être streamée et persistée
     * dans {@code case_analyses.partial_state}. N'est jamais persisté sur
     * {@code AnalysisJob.status} ni sur {@code CaseAnalysis.analysisStatus} : le
     * job reste {@code PROCESSING} pendant tout le streaming, le PARTIAL ne sert
     * qu'au transport SSE.
     */
    PARTIAL,
    DONE,
    FAILED,
    SKIPPED
}
