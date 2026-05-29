package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil F-IM-29-oqtf-categories-l6111-fr sur un
 * dossier pour le contexte de génération de conclusions (SF-JU-02-01).
 *
 * <p>Si une {@link OqtfCategoriesAnalysis} existe pour ce {@code caseFileId},
 * l'outil est considéré comme utilisé sur la branche {@code default} (V1 — sans
 * logique de branche fine).</p>
 */
@Component
public class OqtfCategoriesToolUsageContributor implements ToolUsageContributor {

    private final OqtfCategoriesRepository repository;

    public OqtfCategoriesToolUsageContributor(OqtfCategoriesRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return OqtfCategoriesToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        OqtfCategoriesToolBranchRegistry.TOOL_ID,
                        OqtfCategoriesToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
