package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** SF-223-05 — regime-algerien-be (Famille BELGIQUE). */
@Component
public class RegimeAlgerienBeToolUsageContributor implements ToolUsageContributor {

    private final RegimeAlgerienBeRepository repository;

    public RegimeAlgerienBeToolUsageContributor(RegimeAlgerienBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RegimeAlgerienBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        RegimeAlgerienBeToolBranchRegistry.TOOL_ID,
                        RegimeAlgerienBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
