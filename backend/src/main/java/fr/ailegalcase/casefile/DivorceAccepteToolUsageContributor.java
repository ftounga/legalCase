package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-05 — F-FA-10-divorce-accepte. */
@Component
public class DivorceAccepteToolUsageContributor implements ToolUsageContributor {

    private final DivorceAccepteRepository repository;

    public DivorceAccepteToolUsageContributor(DivorceAccepteRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DivorceAccepteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        DivorceAccepteToolBranchRegistry.TOOL_ID,
                        DivorceAccepteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
