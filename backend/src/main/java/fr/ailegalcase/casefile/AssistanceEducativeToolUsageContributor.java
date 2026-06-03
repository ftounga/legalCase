package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-222-04 — F-FA-ASSISTANCE-EDUCATIVE (assistance éducative, art. 375 et s. Cciv). */
@Component
public class AssistanceEducativeToolUsageContributor implements ToolUsageContributor {

    private final AssistanceEducativeRepository repository;

    public AssistanceEducativeToolUsageContributor(AssistanceEducativeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AssistanceEducativeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        AssistanceEducativeToolBranchRegistry.TOOL_ID,
                        AssistanceEducativeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
