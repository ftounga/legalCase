package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-84 conciliation CPH BCA (FR). */
@Component
public class ConciliationCphBcaToolUsageContributor implements ToolUsageContributor {

    private final ConciliationCphBcaRepository repository;

    public ConciliationCphBcaToolUsageContributor(ConciliationCphBcaRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ConciliationCphBcaToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        ConciliationCphBcaToolBranchRegistry.TOOL_ID,
                        ConciliationCphBcaToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
