package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-71 mutation — validité de la clause de mobilité (FR). */
@Component
public class MutationClauseMobiliteToolUsageContributor implements ToolUsageContributor {

    private final MutationClauseMobiliteRepository repository;

    public MutationClauseMobiliteToolUsageContributor(MutationClauseMobiliteRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return MutationClauseMobiliteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        MutationClauseMobiliteToolBranchRegistry.TOOL_ID,
                        MutationClauseMobiliteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
