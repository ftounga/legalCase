package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de rcc-be-conditions BE via RccBeConditionsAnalysis. */
@Component
public class RccBeConditionsToolUsageContributor implements ToolUsageContributor {
    private final RccBeConditionsRepository repository;

    public RccBeConditionsToolUsageContributor(RccBeConditionsRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RccBeConditionsToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RccBeConditionsToolBranchRegistry.TOOL_ID,
                        RccBeConditionsToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
