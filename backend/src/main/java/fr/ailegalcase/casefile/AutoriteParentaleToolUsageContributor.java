package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-05 — F-FA-19-autorite-parentale. */
@Component
public class AutoriteParentaleToolUsageContributor implements ToolUsageContributor {

    private final AutoriteParentaleRepository repository;

    public AutoriteParentaleToolUsageContributor(AutoriteParentaleRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AutoriteParentaleToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        AutoriteParentaleToolBranchRegistry.TOOL_ID,
                        AutoriteParentaleToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
