package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-02-04-06 — F-FA-11-desunion-irremediable-be. */
@Component
public class DivorceDesunionIrremediableBeToolUsageContributor implements ToolUsageContributor {

    private final DivorceDesunionIrremediableBeRepository repository;

    public DivorceDesunionIrremediableBeToolUsageContributor(DivorceDesunionIrremediableBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DivorceDesunionIrremediableBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        DivorceDesunionIrremediableBeToolBranchRegistry.TOOL_ID,
                        DivorceDesunionIrremediableBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
