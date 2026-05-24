package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-61 protection du lanceur d'alerte (FR). */
@Component
public class LanceurAlerteProtectionToolUsageContributor implements ToolUsageContributor {

    private final LanceurAlerteProtectionRepository repository;

    public LanceurAlerteProtectionToolUsageContributor(LanceurAlerteProtectionRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LanceurAlerteProtectionToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        LanceurAlerteProtectionToolBranchRegistry.TOOL_ID,
                        LanceurAlerteProtectionToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
