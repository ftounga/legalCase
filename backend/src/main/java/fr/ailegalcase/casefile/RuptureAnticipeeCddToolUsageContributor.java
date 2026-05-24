package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-43 rupture anticipée du CDD (FR). */
@Component
public class RuptureAnticipeeCddToolUsageContributor implements ToolUsageContributor {

    private final RuptureAnticipeeCddRepository repository;

    public RuptureAnticipeeCddToolUsageContributor(RuptureAnticipeeCddRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RuptureAnticipeeCddToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        RuptureAnticipeeCddToolBranchRegistry.TOOL_ID,
                        RuptureAnticipeeCddToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
