package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v4 — détecte l'usage de F-FA-22-indivision via IndivisionAnalysis. */
@Component
public class IndivisionToolUsageContributor implements ToolUsageContributor {
    private final IndivisionRepository repository;

    public IndivisionToolUsageContributor(IndivisionRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return IndivisionToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        IndivisionToolBranchRegistry.TOOL_ID,
                        IndivisionToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
