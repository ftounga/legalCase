package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v3 — détecte l'usage de F-FA-15-recompenses via RecompensesAnalysis. */
@Component
public class RecompensesToolUsageContributor implements ToolUsageContributor {
    private final RecompensesRepository repository;

    public RecompensesToolUsageContributor(RecompensesRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RecompensesToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RecompensesToolBranchRegistry.TOOL_ID,
                        RecompensesToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
