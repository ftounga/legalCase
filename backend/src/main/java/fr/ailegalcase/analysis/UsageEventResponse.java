package fr.ailegalcase.analysis;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UsageEventResponse(
        UUID id,
        String eventType,
        int tokensInput,
        int tokensOutput,
        BigDecimal estimatedCost,
        Instant createdAt
) {
    static UsageEventResponse from(UsageEvent event) {
        return new UsageEventResponse(
                event.getId(),
                event.getEventType().name(),
                event.getTokensInput(),
                event.getTokensOutput(),
                event.getEstimatedCost(),
                event.getCreatedAt()
        );
    }
}
