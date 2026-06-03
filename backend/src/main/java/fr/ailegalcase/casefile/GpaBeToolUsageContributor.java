package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-223-04 — gpa-be-situation-contentieuse (Famille BELGIQUE). */
@Component
public class GpaBeToolUsageContributor implements ToolUsageContributor {

    private final GpaBeRepository repository;

    public GpaBeToolUsageContributor(GpaBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return GpaBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        GpaBeToolBranchRegistry.TOOL_ID,
                        GpaBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
