package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v3 — détecte l'usage de F-FA-PARTAGE-NOTARIAL via PartageNotarialAnalysis. */
@Component
public class PartageNotarialToolUsageContributor implements ToolUsageContributor {
    private final PartageNotarialAnalysisRepository repository;

    public PartageNotarialToolUsageContributor(PartageNotarialAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PartageNotarialToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        PartageNotarialToolBranchRegistry.TOOL_ID,
                        PartageNotarialToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
