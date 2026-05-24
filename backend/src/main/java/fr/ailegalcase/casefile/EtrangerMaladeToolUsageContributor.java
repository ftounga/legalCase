package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-JU-03-99c — détecte l'usage de l'outil F-IM-25-etranger-malade-l4259-fr sur un dossier
 * pour le contexte de génération de conclusions (SF-JU-02-01).
 *
 * <p>Si une {@link EtrangerMaladeAnalysis} existe pour ce {@code caseFileId}, l'outil
 * est considéré comme utilisé sur la branche {@code default} (V1 — sans
 * logique de branche fine).</p>
 */
@Component
public class EtrangerMaladeToolUsageContributor implements ToolUsageContributor {

    private final EtrangerMaladeRepository repository;

    public EtrangerMaladeToolUsageContributor(EtrangerMaladeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return EtrangerMaladeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        EtrangerMaladeToolBranchRegistry.TOOL_ID,
                        EtrangerMaladeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
