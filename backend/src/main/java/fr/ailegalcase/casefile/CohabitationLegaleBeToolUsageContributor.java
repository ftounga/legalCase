package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-223-01 — cohabitation-legale-be (Famille BELGIQUE). */
@Component
public class CohabitationLegaleBeToolUsageContributor implements ToolUsageContributor {

    private final CohabitationLegaleBeRepository repository;

    public CohabitationLegaleBeToolUsageContributor(CohabitationLegaleBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CohabitationLegaleBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        CohabitationLegaleBeToolBranchRegistry.TOOL_ID,
                        CohabitationLegaleBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
