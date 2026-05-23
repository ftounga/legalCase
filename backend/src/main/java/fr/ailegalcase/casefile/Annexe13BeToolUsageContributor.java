package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-02-04-06 — F-IM-08-annexe13-be. */
@Component
public class Annexe13BeToolUsageContributor implements ToolUsageContributor {

    private final Annexe13BeRepository repository;

    public Annexe13BeToolUsageContributor(Annexe13BeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return Annexe13BeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        Annexe13BeToolBranchRegistry.TOOL_ID,
                        Annexe13BeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
