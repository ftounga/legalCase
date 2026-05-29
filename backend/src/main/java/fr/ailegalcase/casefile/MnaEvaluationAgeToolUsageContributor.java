package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil F-IM-38-mna-evaluation-age-fr sur un
 * dossier pour le contexte de génération de conclusions (SF-JU-02-01).
 *
 * <p>Si une {@link MnaEvaluationAgeAnalysis} existe pour ce {@code caseFileId},
 * l'outil est considéré comme utilisé sur la branche {@code default}.</p>
 */
@Component
public class MnaEvaluationAgeToolUsageContributor implements ToolUsageContributor {

    private final MnaEvaluationAgeRepository repository;

    public MnaEvaluationAgeToolUsageContributor(MnaEvaluationAgeRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return MnaEvaluationAgeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(a -> new ToolUsage(
                        MnaEvaluationAgeToolBranchRegistry.TOOL_ID,
                        MnaEvaluationAgeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
