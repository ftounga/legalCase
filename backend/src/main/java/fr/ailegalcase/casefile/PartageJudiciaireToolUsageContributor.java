package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v3 — détecte l'usage de F-FA-17-partage-judiciaire via PartageJudiciaireAnalysis. */
@Component
public class PartageJudiciaireToolUsageContributor implements ToolUsageContributor {
    private final PartageJudiciaireRepository repository;

    public PartageJudiciaireToolUsageContributor(PartageJudiciaireRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PartageJudiciaireToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        PartageJudiciaireToolBranchRegistry.TOOL_ID,
                        PartageJudiciaireToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
