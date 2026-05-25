package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de liquidation-partage-be BE via LiquidationPartageBeAnalysis. */
@Component
public class LiquidationPartageBeToolUsageContributor implements ToolUsageContributor {
    private final LiquidationPartageBeRepository repository;

    public LiquidationPartageBeToolUsageContributor(LiquidationPartageBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LiquidationPartageBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        LiquidationPartageBeToolBranchRegistry.TOOL_ID,
                        LiquidationPartageBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
