package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e — détecte l'usage de F-FA-RETRAIT-AP via RetraitApAnalysis. */
@Component
public class RetraitApToolUsageContributor implements ToolUsageContributor {
    private final RetraitApAnalysisRepository repository;

    public RetraitApToolUsageContributor(RetraitApAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RetraitApToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RetraitApToolBranchRegistry.TOOL_ID,
                        RetraitApToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
