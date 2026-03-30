package fr.ailegalcase.superadmin;

public record SuperAdminMetricsResponse(
        long totalWorkspaces,
        long activeWorkspaces30d,
        long inactiveWorkspaces30d,
        long trialWorkspaces,
        long paidWorkspaces,
        double conversionRatePct,
        long analysesLast7Days,
        long analysesLast30Days,
        long newWorkspacesLast30Days
) {}
