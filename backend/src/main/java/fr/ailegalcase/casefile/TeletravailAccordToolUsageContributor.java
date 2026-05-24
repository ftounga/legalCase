package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-82 télétravail — conformité et litige (FR). */
@Component
public class TeletravailAccordToolUsageContributor implements ToolUsageContributor {

    private final TeletravailAccordRepository repository;

    public TeletravailAccordToolUsageContributor(TeletravailAccordRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return TeletravailAccordToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        TeletravailAccordToolBranchRegistry.TOOL_ID,
                        TeletravailAccordToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
