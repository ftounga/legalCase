package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil F-IM-50-pacs-vpf-fr sur un dossier pour
 * le contexte de génération de conclusions.
 *
 * <p>Si une {@link PacsVpfAnalysis} existe pour ce {@code caseFileId}, l'outil
 * est considéré comme utilisé sur la branche {@code default}.</p>
 */
@Component
public class PacsVpfToolUsageContributor implements ToolUsageContributor {

    private final PacsVpfRepository repository;

    public PacsVpfToolUsageContributor(PacsVpfRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PacsVpfToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        PacsVpfToolBranchRegistry.TOOL_ID,
                        PacsVpfToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
