package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-30-aesm-mena-be (F-215 P2 Immigration BE / SF-215-11). */
@Component
public class AesmMenaBeToolUsageContributor implements ToolUsageContributor {

    private final AesmMenaBeRepository repository;

    public AesmMenaBeToolUsageContributor(AesmMenaBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AesmMenaBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        AesmMenaBeToolBranchRegistry.TOOL_ID,
                        AesmMenaBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
