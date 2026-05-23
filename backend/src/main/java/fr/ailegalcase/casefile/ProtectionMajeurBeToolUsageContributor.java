package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-02-04-06 — protection-majeur-be. */
@Component
public class ProtectionMajeurBeToolUsageContributor implements ToolUsageContributor {

    private final ProtectionMajeurBeRepository repository;

    public ProtectionMajeurBeToolUsageContributor(ProtectionMajeurBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ProtectionMajeurBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ProtectionMajeurBeToolBranchRegistry.TOOL_ID,
                        ProtectionMajeurBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
