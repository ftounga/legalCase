package fr.ailegalcase.dashboard;

import java.time.LocalDate;
import java.util.UUID;

public record DashboardDeadlineItem(UUID id, String label, LocalDate dueDate, UUID caseFileId, String caseFileTitle) {
}
