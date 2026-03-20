package fr.ailegalcase.superadmin;

import java.math.BigDecimal;
import java.util.UUID;

public record SuperAdminUsageResponse(
        UUID workspaceId,
        String workspaceName,
        long totalTokensInput,
        long totalTokensOutput,
        BigDecimal totalCost
) {}
