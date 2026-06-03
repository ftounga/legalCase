package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil F-IM-52-signalement-sis-fr sur un dossier
 * pour le contexte de génération de conclusions.
 *
 * <p>Si une {@link SignalementSisAnalysis} existe pour ce {@code caseFileId},
 * l'outil est considéré comme utilisé sur la branche {@code default}.</p>
 */
@Component
public class SignalementSisToolUsageContributor implements ToolUsageContributor {

    private final SignalementSisRepository repository;

    public SignalementSisToolUsageContributor(SignalementSisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return SignalementSisToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        SignalementSisToolBranchRegistry.TOOL_ID,
                        SignalementSisToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
