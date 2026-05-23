package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-05 — F-FA-07-checklist-divorce. */
@Component
public class DivorceChecklistToolUsageContributor implements ToolUsageContributor {

    private final DivorceChecklistRepository repository;

    public DivorceChecklistToolUsageContributor(DivorceChecklistRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DivorceChecklistToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        DivorceChecklistToolBranchRegistry.TOOL_ID,
                        DivorceChecklistToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
