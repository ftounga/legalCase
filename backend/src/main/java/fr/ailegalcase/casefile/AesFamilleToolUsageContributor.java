package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-03 — F-IM-09-aes-famille. */
@Component
public class AesFamilleToolUsageContributor implements ToolUsageContributor {

    private final AesFamilleRepository repository;

    public AesFamilleToolUsageContributor(AesFamilleRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AesFamilleToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        AesFamilleToolBranchRegistry.TOOL_ID,
                        AesFamilleToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
