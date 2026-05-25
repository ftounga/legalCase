package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** F-JU-03 / SF-JU-03-99e v4 — détecte l'usage de F-FA-ARIPA-RECOUVREMENT via AripaRecouvrementFrAnalysis. */
@Component
public class AripaRecouvrementFrToolUsageContributor implements ToolUsageContributor {
    private final AripaRecouvrementFrRepository repository;

    public AripaRecouvrementFrToolUsageContributor(AripaRecouvrementFrRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AripaRecouvrementFrToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        AripaRecouvrementFrToolBranchRegistry.TOOL_ID,
                        AripaRecouvrementFrToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
