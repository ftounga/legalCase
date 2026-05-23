package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-JU-03-01 — détecte l'usage du F-DT-10 validité rupture
 * conventionnelle sur un dossier pour le contexte F-98 conclusions (SF-JU-02-01).
 */
@Component
public class RuptureConvToolUsageContributor implements ToolUsageContributor {

    private final RuptureConvAnalysisRepository repository;

    public RuptureConvToolUsageContributor(RuptureConvAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RuptureConvToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        RuptureConvToolBranchRegistry.TOOL_ID,
                        RuptureConvToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
