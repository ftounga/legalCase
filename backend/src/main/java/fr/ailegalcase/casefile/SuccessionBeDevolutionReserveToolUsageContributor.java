package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de succession-be-devolution-reserve BE via SuccessionBeDevolutionReserveAnalysis. */
@Component
public class SuccessionBeDevolutionReserveToolUsageContributor implements ToolUsageContributor {
    private final SuccessionBeDevolutionReserveRepository repository;

    public SuccessionBeDevolutionReserveToolUsageContributor(SuccessionBeDevolutionReserveRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return SuccessionBeDevolutionReserveToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        SuccessionBeDevolutionReserveToolBranchRegistry.TOOL_ID,
                        SuccessionBeDevolutionReserveToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
