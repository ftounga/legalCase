package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil F-IM-32-recepisse-attestation-fr sur un
 * dossier pour le contexte de génération de conclusions (SF-JU-02-01).
 *
 * <p>Si une {@link RecepisseAttestationAnalysis} existe pour ce {@code caseFileId},
 * l'outil est considéré comme utilisé sur la branche {@code default} (V1 — sans
 * logique de branche fine).</p>
 */
@Component
public class RecepisseAttestationToolUsageContributor implements ToolUsageContributor {

    private final RecepisseAttestationRepository repository;

    public RecepisseAttestationToolUsageContributor(RecepisseAttestationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RecepisseAttestationToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        RecepisseAttestationToolBranchRegistry.TOOL_ID,
                        RecepisseAttestationToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
