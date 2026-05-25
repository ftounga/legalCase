package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v3 — détecte l'usage de F-FA-DONATION-ENTRE-EPOUX via DonationEntreEpouxAnalysis. */
@Component
public class DonationEntreEpouxToolUsageContributor implements ToolUsageContributor {
    private final DonationEntreEpouxAnalysisRepository repository;

    public DonationEntreEpouxToolUsageContributor(DonationEntreEpouxAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DonationEntreEpouxToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        DonationEntreEpouxToolBranchRegistry.TOOL_ID,
                        DonationEntreEpouxToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
