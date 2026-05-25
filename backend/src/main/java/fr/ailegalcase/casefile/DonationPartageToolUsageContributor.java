package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v3 — détecte l'usage de F-FA-DONATION-PARTAGE via DonationPartageAnalysis. */
@Component
public class DonationPartageToolUsageContributor implements ToolUsageContributor {
    private final DonationPartageAnalysisRepository repository;

    public DonationPartageToolUsageContributor(DonationPartageAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DonationPartageToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        DonationPartageToolBranchRegistry.TOOL_ID,
                        DonationPartageToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
