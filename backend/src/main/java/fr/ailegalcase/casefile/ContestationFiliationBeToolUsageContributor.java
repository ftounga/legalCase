package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-02-04-06 — contestation-filiation-be. */
@Component
public class ContestationFiliationBeToolUsageContributor implements ToolUsageContributor {

    private final ContestationFiliationBeRepository repository;

    public ContestationFiliationBeToolUsageContributor(ContestationFiliationBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ContestationFiliationBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ContestationFiliationBeToolBranchRegistry.TOOL_ID,
                        ContestationFiliationBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
