package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil F-IM-47-regime-tunisien-fr sur un dossier
 * pour le contexte de génération de conclusions.
 *
 * <p>Si une {@link RegimeTunisienAnalysis} existe pour ce {@code caseFileId},
 * l'outil est considéré comme utilisé sur la branche {@code default}.</p>
 */
@Component
public class RegimeTunisienToolUsageContributor implements ToolUsageContributor {

    private final RegimeTunisienRepository repository;

    public RegimeTunisienToolUsageContributor(RegimeTunisienRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RegimeTunisienToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        RegimeTunisienToolBranchRegistry.TOOL_ID,
                        RegimeTunisienToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
