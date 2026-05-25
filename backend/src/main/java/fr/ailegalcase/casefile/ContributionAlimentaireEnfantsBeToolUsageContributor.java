package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de contribution-alimentaire-enfants-be BE via ContributionAlimentaireEnfantsBeAnalysis. */
@Component
public class ContributionAlimentaireEnfantsBeToolUsageContributor implements ToolUsageContributor {
    private final ContributionAlimentaireEnfantsBeRepository repository;

    public ContributionAlimentaireEnfantsBeToolUsageContributor(ContributionAlimentaireEnfantsBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ContributionAlimentaireEnfantsBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        ContributionAlimentaireEnfantsBeToolBranchRegistry.TOOL_ID,
                        ContributionAlimentaireEnfantsBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
