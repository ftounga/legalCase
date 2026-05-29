package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil F-IM-34-aj-cnda-fr sur un dossier pour le
 * contexte de génération de conclusions (SF-JU-02-01).
 *
 * <p>Si une {@link AjCndaAnalysis} existe pour ce {@code caseFileId}, l'outil est
 * considéré comme utilisé sur la branche {@code default} (V1 — sans logique de
 * branche fine).</p>
 */
@Component
public class AjCndaToolUsageContributor implements ToolUsageContributor {

    private final AjCndaRepository repository;

    public AjCndaToolUsageContributor(AjCndaRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AjCndaToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        AjCndaToolBranchRegistry.TOOL_ID,
                        AjCndaToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
