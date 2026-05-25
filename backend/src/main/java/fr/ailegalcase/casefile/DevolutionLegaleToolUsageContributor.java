package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99d — détecte l'usage de F-FA-24-devolution-legale via DevolutionLegaleAnalysis. */
@Component
public class DevolutionLegaleToolUsageContributor implements ToolUsageContributor {
    private final DevolutionLegaleRepository repository;

    public DevolutionLegaleToolUsageContributor(DevolutionLegaleRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DevolutionLegaleToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        DevolutionLegaleToolBranchRegistry.TOOL_ID,
                        DevolutionLegaleToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
