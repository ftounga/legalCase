package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99d — détecte l'usage de F-FA-24-indivision-successorale via IndivisionSuccessoraleAnalysis. */
@Component
public class IndivisionSuccessoraleToolUsageContributor implements ToolUsageContributor {
    private final IndivisionSuccessoraleRepository repository;

    public IndivisionSuccessoraleToolUsageContributor(IndivisionSuccessoraleRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return IndivisionSuccessoraleToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        IndivisionSuccessoraleToolBranchRegistry.TOOL_ID,
                        IndivisionSuccessoraleToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
