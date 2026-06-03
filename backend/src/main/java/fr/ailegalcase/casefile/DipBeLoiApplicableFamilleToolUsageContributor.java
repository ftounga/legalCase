package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-223-07 — dip-be-loi-applicable-famille (Famille BELGIQUE). */
@Component
public class DipBeLoiApplicableFamilleToolUsageContributor implements ToolUsageContributor {

    private final DipBeLoiApplicableFamilleRepository repository;

    public DipBeLoiApplicableFamilleToolUsageContributor(DipBeLoiApplicableFamilleRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DipBeLoiApplicableFamilleToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        DipBeLoiApplicableFamilleToolBranchRegistry.TOOL_ID,
                        DipBeLoiApplicableFamilleToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
