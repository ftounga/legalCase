package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de contribution-conjoint-be BE via ContributionConjointBeAnalysis. */
@Component
public class ContributionConjointBeToolUsageContributor implements ToolUsageContributor {
    private final ContributionConjointBeRepository repository;

    public ContributionConjointBeToolUsageContributor(ContributionConjointBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ContributionConjointBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        ContributionConjointBeToolBranchRegistry.TOOL_ID,
                        ContributionConjointBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
