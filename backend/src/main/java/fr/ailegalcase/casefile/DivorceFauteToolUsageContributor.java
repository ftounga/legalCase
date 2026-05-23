package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-05 — F-FA-09 divorce pour faute. */
@Component
public class DivorceFauteToolUsageContributor implements ToolUsageContributor {

    private final DivorceFauteRepository repository;

    public DivorceFauteToolUsageContributor(DivorceFauteRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DivorceFauteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        DivorceFauteToolBranchRegistry.TOOL_ID,
                        DivorceFauteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
