package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99f — détecte l'usage de autorite-parentale-be BE via AutoriteParentaleBeAnalysis. */
@Component
public class AutoriteParentaleBeToolUsageContributor implements ToolUsageContributor {
    private final AutoriteParentaleBeRepository repository;

    public AutoriteParentaleBeToolUsageContributor(AutoriteParentaleBeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AutoriteParentaleBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        AutoriteParentaleBeToolBranchRegistry.TOOL_ID,
                        AutoriteParentaleBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
