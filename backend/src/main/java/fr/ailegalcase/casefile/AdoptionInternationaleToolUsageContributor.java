package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e — détecte l'usage de F-FA-ADOPTION-INTERNATIONALE via AdoptionInternationaleAnalysis. */
@Component
public class AdoptionInternationaleToolUsageContributor implements ToolUsageContributor {
    private final AdoptionInternationaleAnalysisRepository repository;

    public AdoptionInternationaleToolUsageContributor(AdoptionInternationaleAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AdoptionInternationaleToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        AdoptionInternationaleToolBranchRegistry.TOOL_ID,
                        AdoptionInternationaleToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
