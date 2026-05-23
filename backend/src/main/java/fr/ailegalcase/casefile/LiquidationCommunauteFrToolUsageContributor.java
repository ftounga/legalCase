package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-05 — F-FA-04-liquidation-communaute. */
@Component
public class LiquidationCommunauteFrToolUsageContributor implements ToolUsageContributor {

    private final LiquidationCommunauteFrRepository repository;

    public LiquidationCommunauteFrToolUsageContributor(LiquidationCommunauteFrRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LiquidationCommunauteFrToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        LiquidationCommunauteFrToolBranchRegistry.TOOL_ID,
                        LiquidationCommunauteFrToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
