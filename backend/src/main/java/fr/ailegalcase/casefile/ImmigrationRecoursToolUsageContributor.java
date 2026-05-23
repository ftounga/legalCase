package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-03 — F-IM-06-recours. */
@Component
public class ImmigrationRecoursToolUsageContributor implements ToolUsageContributor {

    private final ImmigrationRecoursRepository repository;

    public ImmigrationRecoursToolUsageContributor(ImmigrationRecoursRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ImmigrationRecoursToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ImmigrationRecoursToolBranchRegistry.TOOL_ID,
                        ImmigrationRecoursToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
