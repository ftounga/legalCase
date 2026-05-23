package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-02-04-06 — F-DT-28-avantages-conventionnels-be. */
@Component
public class AvantagesConventionnelsBeToolUsageContributor implements ToolUsageContributor {

    private final AvantagesConventionnelsBeRepository repository;

    public AvantagesConventionnelsBeToolUsageContributor(AvantagesConventionnelsBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AvantagesConventionnelsBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        AvantagesConventionnelsBeToolBranchRegistry.TOOL_ID,
                        AvantagesConventionnelsBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
