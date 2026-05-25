package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-64 burn-out reconnaissance MP (FR). */
@Component
public class BurnoutReconnaissanceMpToolUsageContributor implements ToolUsageContributor {

    private final BurnoutReconnaissanceMpRepository repository;

    public BurnoutReconnaissanceMpToolUsageContributor(BurnoutReconnaissanceMpRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return BurnoutReconnaissanceMpToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        BurnoutReconnaissanceMpToolBranchRegistry.TOOL_ID,
                        BurnoutReconnaissanceMpToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
