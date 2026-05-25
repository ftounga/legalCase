package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99d — détecte l'usage de F-FA-24-donation via DonationAnalysis. */
@Component
public class DonationToolUsageContributor implements ToolUsageContributor {
    private final DonationRepository repository;

    public DonationToolUsageContributor(DonationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DonationToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        DonationToolBranchRegistry.TOOL_ID,
                        DonationToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
