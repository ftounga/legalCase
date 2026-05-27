package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-IM-25-single-permit-be (F-215 P2 Immigration BE / SF-215-01). */
@Component
public class SinglePermitBeToolUsageContributor implements ToolUsageContributor {

    private final SinglePermitBeRepository repository;

    public SinglePermitBeToolUsageContributor(SinglePermitBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return SinglePermitBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        SinglePermitBeToolBranchRegistry.TOOL_ID,
                        SinglePermitBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
