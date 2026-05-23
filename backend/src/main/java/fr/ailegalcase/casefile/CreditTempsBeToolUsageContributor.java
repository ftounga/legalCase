package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-02-04-06 — F-DT-29-credit-temps-be. */
@Component
public class CreditTempsBeToolUsageContributor implements ToolUsageContributor {

    private final CreditTempsBeRepository repository;

    public CreditTempsBeToolUsageContributor(CreditTempsBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CreditTempsBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        CreditTempsBeToolBranchRegistry.TOOL_ID,
                        CreditTempsBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
