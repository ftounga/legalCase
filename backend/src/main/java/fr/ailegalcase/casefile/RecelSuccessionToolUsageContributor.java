package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99d — détecte l'usage de F-FA-RECEL-SUCCESSION via RecelSuccessionAnalysis. */
@Component
public class RecelSuccessionToolUsageContributor implements ToolUsageContributor {
    private final RecelSuccessionAnalysisRepository repository;

    public RecelSuccessionToolUsageContributor(RecelSuccessionAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RecelSuccessionToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RecelSuccessionToolBranchRegistry.TOOL_ID,
                        RecelSuccessionToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
