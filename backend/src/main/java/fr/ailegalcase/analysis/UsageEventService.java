package fr.ailegalcase.analysis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class UsageEventService {

    private final UsageEventRepository repository;
    private final double costPerInputToken;
    private final double costPerOutputToken;

    public UsageEventService(UsageEventRepository repository,
                             @Value("${anthropic.cost-per-input-token:0.000003}") double costPerInputToken,
                             @Value("${anthropic.cost-per-output-token:0.000015}") double costPerOutputToken) {
        this.repository = repository;
        this.costPerInputToken = costPerInputToken;
        this.costPerOutputToken = costPerOutputToken;
    }

    public void record(UUID caseFileId, UUID userId, JobType eventType, int tokensInput, int tokensOutput) {
        double rawCost = tokensInput * costPerInputToken + tokensOutput * costPerOutputToken;
        BigDecimal estimatedCost = BigDecimal.valueOf(rawCost).setScale(6, RoundingMode.HALF_UP);

        UsageEvent event = new UsageEvent();
        event.setCaseFileId(caseFileId);
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setTokensInput(tokensInput);
        event.setTokensOutput(tokensOutput);
        event.setEstimatedCost(estimatedCost);
        repository.save(event);
    }
}
