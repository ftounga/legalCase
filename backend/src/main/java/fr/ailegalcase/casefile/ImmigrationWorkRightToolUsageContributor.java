package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-03 — F-IM-07-droit-au-travail. */
@Component
public class ImmigrationWorkRightToolUsageContributor implements ToolUsageContributor {

    private final ImmigrationWorkRightRepository repository;

    public ImmigrationWorkRightToolUsageContributor(ImmigrationWorkRightRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ImmigrationWorkRightToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ImmigrationWorkRightToolBranchRegistry.TOOL_ID,
                        ImmigrationWorkRightToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
