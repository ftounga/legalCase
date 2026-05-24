package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-46 PDV / RCC conformité (FR). */
@Component
public class PdvRccConformiteToolUsageContributor implements ToolUsageContributor {

    private final PdvRccConformiteRepository repository;

    public PdvRccConformiteToolUsageContributor(PdvRccConformiteRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PdvRccConformiteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        PdvRccConformiteToolBranchRegistry.TOOL_ID,
                        PdvRccConformiteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
