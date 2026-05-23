package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-03 — F-IM-08-referes-admin-fr. */
@Component
public class ReferesAdminToolUsageContributor implements ToolUsageContributor {

    private final ReferesAdminRepository repository;

    public ReferesAdminToolUsageContributor(ReferesAdminRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ReferesAdminToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ReferesAdminToolBranchRegistry.TOOL_ID,
                        ReferesAdminToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
