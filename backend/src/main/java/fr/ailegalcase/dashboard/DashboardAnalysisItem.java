package fr.ailegalcase.dashboard;

import java.time.Instant;
import java.util.UUID;

public record DashboardAnalysisItem(UUID id, UUID caseFileId, String caseFileTitle, String analysisType,
                                    Instant createdAt) {
}
