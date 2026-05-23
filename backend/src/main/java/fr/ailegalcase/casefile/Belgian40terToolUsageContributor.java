package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-02-04-06 — F-IM-14-40ter-familial-belge-be. */
@Component
public class Belgian40terToolUsageContributor implements ToolUsageContributor {

    private final Belgian40terRepository repository;

    public Belgian40terToolUsageContributor(Belgian40terRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return Belgian40terToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        Belgian40terToolBranchRegistry.TOOL_ID,
                        Belgian40terToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
