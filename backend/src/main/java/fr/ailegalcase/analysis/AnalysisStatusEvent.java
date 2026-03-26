package fr.ailegalcase.analysis;

import java.util.UUID;

public record AnalysisStatusEvent(UUID caseFileId, AnalysisStatus status, JobType jobType) {}
