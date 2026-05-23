package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-02-04-06 — F-IM-14-40bis-cohabitant-ue-be. */
@Component
public class Belgian40bisToolUsageContributor implements ToolUsageContributor {

    private final Belgian40bisRepository repository;

    public Belgian40bisToolUsageContributor(Belgian40bisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return Belgian40bisToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        Belgian40bisToolBranchRegistry.TOOL_ID,
                        Belgian40bisToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
