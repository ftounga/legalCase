package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v4 — détecte l'usage de F-FA-20-pacs-dissolution via PacsDissolutionAnalysis. */
@Component
public class PacsDissolutionToolUsageContributor implements ToolUsageContributor {
    private final PacsDissolutionRepository repository;

    public PacsDissolutionToolUsageContributor(PacsDissolutionRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PacsDissolutionToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        PacsDissolutionToolBranchRegistry.TOOL_ID,
                        PacsDissolutionToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
