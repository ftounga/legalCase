package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e — détecte l'usage de F-FA-PRESOMPTION-PATERNITE via PresomptionPaterniteAnalysis. */
@Component
public class PresomptionPaterniteToolUsageContributor implements ToolUsageContributor {
    private final PresomptionPaterniteAnalysisRepository repository;

    public PresomptionPaterniteToolUsageContributor(PresomptionPaterniteAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PresomptionPaterniteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        PresomptionPaterniteToolBranchRegistry.TOOL_ID,
                        PresomptionPaterniteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
