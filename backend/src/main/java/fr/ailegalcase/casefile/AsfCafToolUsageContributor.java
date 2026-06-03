package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-222-01 — F-FA-ASF-CAF (allocation de soutien familial). */
@Component
public class AsfCafToolUsageContributor implements ToolUsageContributor {

    private final AsfCafRepository repository;

    public AsfCafToolUsageContributor(AsfCafRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AsfCafToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        AsfCafToolBranchRegistry.TOOL_ID,
                        AsfCafToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
