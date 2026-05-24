package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-JU-03-99c — détecte l'usage de l'outil F-IM-24-victime-violences-l4256-fr sur un dossier
 * pour le contexte de génération de conclusions (SF-JU-02-01).
 *
 * <p>Si une {@link VictimeViolencesL4256Analysis} existe pour ce {@code caseFileId}, l'outil
 * est considéré comme utilisé sur la branche {@code default} (V1 — sans
 * logique de branche fine).</p>
 */
@Component
public class VictimeViolencesL4256ToolUsageContributor implements ToolUsageContributor {

    private final VictimeViolencesL4256Repository repository;

    public VictimeViolencesL4256ToolUsageContributor(VictimeViolencesL4256Repository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return VictimeViolencesL4256ToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        VictimeViolencesL4256ToolBranchRegistry.TOOL_ID,
                        VictimeViolencesL4256ToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
