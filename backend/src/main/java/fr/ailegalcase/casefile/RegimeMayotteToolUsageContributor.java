package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil F-IM-48-regime-mayotte-fr sur un dossier
 * pour le contexte de génération de conclusions.
 *
 * <p>Si une {@link RegimeMayotteAnalysis} existe pour ce {@code caseFileId},
 * l'outil est considéré comme utilisé sur la branche {@code default}.</p>
 */
@Component
public class RegimeMayotteToolUsageContributor implements ToolUsageContributor {

    private final RegimeMayotteRepository repository;

    public RegimeMayotteToolUsageContributor(RegimeMayotteRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RegimeMayotteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        RegimeMayotteToolBranchRegistry.TOOL_ID,
                        RegimeMayotteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
