package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 — F-DT-77 congé maternité / paternité (FR). */
@Component
public class CongeMaternitePaterniteToolUsageContributor implements ToolUsageContributor {

    private final CongeMaternitePaterniteRepository repository;

    public CongeMaternitePaterniteToolUsageContributor(CongeMaternitePaterniteRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CongeMaternitePaterniteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        CongeMaternitePaterniteToolBranchRegistry.TOOL_ID,
                        CongeMaternitePaterniteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
