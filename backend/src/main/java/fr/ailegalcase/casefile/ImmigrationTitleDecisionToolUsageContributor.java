package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-03 — F-IM-05-arbre-decisionnel-titre. */
@Component
public class ImmigrationTitleDecisionToolUsageContributor implements ToolUsageContributor {

    private final ImmigrationTitleDecisionRepository repository;

    public ImmigrationTitleDecisionToolUsageContributor(ImmigrationTitleDecisionRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ImmigrationTitleDecisionToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ImmigrationTitleDecisionToolBranchRegistry.TOOL_ID,
                        ImmigrationTitleDecisionToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
