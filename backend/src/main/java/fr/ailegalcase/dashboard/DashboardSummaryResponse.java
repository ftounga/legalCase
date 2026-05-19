package fr.ailegalcase.dashboard;

import java.util.List;

public record DashboardSummaryResponse(
        List<DashboardOpenCaseItem> openCases,
        long openCasesCount,
        List<DashboardDeadlineItem> urgentDeadlines,
        List<DashboardStaleCheckItem> staleChecks,
        List<DashboardAnalysisItem> recentAnalyses,
        String userFirstName,
        long casesOpenedThisWeek,
        List<DashboardActivityDayItem> weeklyActivity
) {
}
