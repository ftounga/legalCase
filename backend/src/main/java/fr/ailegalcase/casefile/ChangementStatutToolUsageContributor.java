package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-03 — F-IM-11-changement-statut. */
@Component
public class ChangementStatutToolUsageContributor implements ToolUsageContributor {

    private final ChangementStatutRepository repository;

    public ChangementStatutToolUsageContributor(ChangementStatutRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ChangementStatutToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ChangementStatutToolBranchRegistry.TOOL_ID,
                        ChangementStatutToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
