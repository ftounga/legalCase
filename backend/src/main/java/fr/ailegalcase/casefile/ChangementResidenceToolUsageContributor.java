package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e — détecte l'usage de F-FA-19-changement-residence via ChangementResidenceAnalysis. */
@Component
public class ChangementResidenceToolUsageContributor implements ToolUsageContributor {
    private final ChangementResidenceRepository repository;

    public ChangementResidenceToolUsageContributor(ChangementResidenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ChangementResidenceToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        ChangementResidenceToolBranchRegistry.TOOL_ID,
                        ChangementResidenceToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
