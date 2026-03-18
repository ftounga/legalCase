package fr.ailegalcase.analysis;

import java.math.BigDecimal;
import java.util.UUID;

public record UserUsageSummary(
        UUID userId,
        String userEmail,
        int tokensInput,
        int tokensOutput,
        BigDecimal totalCost
) {}
