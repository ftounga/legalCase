package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e — détecte l'usage de F-FA-ADOPTION-INTRA via AdoptionIntraFrAnalysis. */
@Component
public class AdoptionIntraFrToolUsageContributor implements ToolUsageContributor {
    private final AdoptionIntraFrRepository repository;

    public AdoptionIntraFrToolUsageContributor(AdoptionIntraFrRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AdoptionIntraFrToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        AdoptionIntraFrToolBranchRegistry.TOOL_ID,
                        AdoptionIntraFrToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
