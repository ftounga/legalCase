package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99d — détecte l'usage de F-FA-INDIGNITE-SUCCESSORALE via IndigniteSuccessoraleAnalysis. */
@Component
public class IndigniteSuccessoraleToolUsageContributor implements ToolUsageContributor {
    private final IndigniteSuccessoraleAnalysisRepository repository;

    public IndigniteSuccessoraleToolUsageContributor(IndigniteSuccessoraleAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return IndigniteSuccessoraleToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        IndigniteSuccessoraleToolBranchRegistry.TOOL_ID,
                        IndigniteSuccessoraleToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
