package fr.ailegalcase.dashboard;

import java.util.UUID;

public record DashboardOpenCaseItem(UUID id, String title, String legalDomain, String status) {
}
