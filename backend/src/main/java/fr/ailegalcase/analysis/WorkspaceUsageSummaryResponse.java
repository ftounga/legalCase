package fr.ailegalcase.analysis;

import java.math.BigDecimal;
import java.util.List;

public record WorkspaceUsageSummaryResponse(
        int totalTokensInput,
        int totalTokensOutput,
        BigDecimal totalCost,
        List<UserUsageSummary> byUser,
        List<CaseFileUsageSummary> byCaseFile
) {}
