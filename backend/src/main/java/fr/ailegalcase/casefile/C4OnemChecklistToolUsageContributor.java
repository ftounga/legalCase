package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de c4-onem-checklist BE via C4OnemChecklistAnalysis. */
@Component
public class C4OnemChecklistToolUsageContributor implements ToolUsageContributor {
    private final C4OnemChecklistRepository repository;

    public C4OnemChecklistToolUsageContributor(C4OnemChecklistRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return C4OnemChecklistToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        C4OnemChecklistToolBranchRegistry.TOOL_ID,
                        C4OnemChecklistToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
