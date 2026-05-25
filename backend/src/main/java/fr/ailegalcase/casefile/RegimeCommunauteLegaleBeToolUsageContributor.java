package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de regime-mat-be-communaute-legale BE via RegimeCommunauteLegaleBeAnalysis. */
@Component
public class RegimeCommunauteLegaleBeToolUsageContributor implements ToolUsageContributor {
    private final RegimeCommunauteLegaleBeRepository repository;

    public RegimeCommunauteLegaleBeToolUsageContributor(RegimeCommunauteLegaleBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RegimeCommunauteLegaleBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RegimeCommunauteLegaleBeToolBranchRegistry.TOOL_ID,
                        RegimeCommunauteLegaleBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
