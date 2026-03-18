package fr.ailegalcase.analysis;

import java.math.BigDecimal;
import java.util.UUID;

public record CaseFileUsageSummary(
        UUID caseFileId,
        String caseFileTitle,
        int tokensInput,
        int tokensOutput,
        BigDecimal totalCost
) {}
