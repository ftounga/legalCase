package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-70 modification du contrat — refus du salarié (FR). */
@Component
public class ModificationContratRefusToolUsageContributor implements ToolUsageContributor {

    private final ModificationContratRefusRepository repository;

    public ModificationContratRefusToolUsageContributor(ModificationContratRefusRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ModificationContratRefusToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ModificationContratRefusToolBranchRegistry.TOOL_ID,
                        ModificationContratRefusToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
