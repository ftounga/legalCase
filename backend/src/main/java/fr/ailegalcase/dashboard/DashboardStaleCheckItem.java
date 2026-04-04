package fr.ailegalcase.dashboard;

import java.util.UUID;

public record DashboardStaleCheckItem(UUID caseFileId, String caseFileTitle, long nonCompliantCount) {
}
