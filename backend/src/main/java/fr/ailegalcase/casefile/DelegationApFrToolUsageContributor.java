package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e — détecte l'usage de F-FA-XX-delegation-ap via DelegationApFrAnalysis. */
@Component
public class DelegationApFrToolUsageContributor implements ToolUsageContributor {
    private final DelegationApFrRepository repository;

    public DelegationApFrToolUsageContributor(DelegationApFrRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DelegationApFrToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        DelegationApFrToolBranchRegistry.TOOL_ID,
                        DelegationApFrToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
