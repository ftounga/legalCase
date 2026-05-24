package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99c v2 — détecte l'usage de F-IM-19-mineurs via MineursImmigrationAnalysis. */
@Component
public class MineursImmigrationToolUsageContributor implements ToolUsageContributor {
    private final MineursImmigrationRepository repository;

    public MineursImmigrationToolUsageContributor(MineursImmigrationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return MineursImmigrationToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        MineursImmigrationToolBranchRegistry.TOOL_ID,
                        MineursImmigrationToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
