package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-44 CSP/CRP conformité FR. */
@Component
public class CspCrpConformiteToolUsageContributor implements ToolUsageContributor {

    private final CspCrpConformiteRepository repository;

    public CspCrpConformiteToolUsageContributor(CspCrpConformiteRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CspCrpConformiteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        CspCrpConformiteToolBranchRegistry.TOOL_ID,
                        CspCrpConformiteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
