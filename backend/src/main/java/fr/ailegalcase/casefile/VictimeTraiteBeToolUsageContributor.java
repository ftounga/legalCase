package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-58-victime-traite-be (F-221 P3 Immigration BE / SF-221-06). */
@Component
public class VictimeTraiteBeToolUsageContributor implements ToolUsageContributor {

    private final VictimeTraiteBeRepository repository;

    public VictimeTraiteBeToolUsageContributor(VictimeTraiteBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return VictimeTraiteBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        VictimeTraiteBeToolBranchRegistry.TOOL_ID,
                        VictimeTraiteBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
