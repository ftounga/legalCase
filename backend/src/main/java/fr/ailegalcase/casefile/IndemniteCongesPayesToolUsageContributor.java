package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-01 — F-DT-26 indemnité congés payés. */
@Component
public class IndemniteCongesPayesToolUsageContributor implements ToolUsageContributor {

    private final IndemniteCongesPayesRepository repository;

    public IndemniteCongesPayesToolUsageContributor(IndemniteCongesPayesRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return IndemniteCongesPayesToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        IndemniteCongesPayesToolBranchRegistry.TOOL_ID,
                        IndemniteCongesPayesToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
