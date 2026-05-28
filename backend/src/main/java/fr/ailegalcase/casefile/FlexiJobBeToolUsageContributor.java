package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-12 — détecte l'usage de l'outil
 * {@code flexi-job-be} (BELGIQUE) via la présence d'une
 * {@link FlexiJobBeAnalysis} pour le {@code caseFileId} considéré.
 * Permet au mapping jurisprudence de remonter les arrêts pertinents
 * (Cour const. arrêt 107/2017 sur l'extension boulangerie/coiffure ;
 * Cass. BE en matière de requalification ONSS de prestations flexi)
 * dans la synthèse de dossier.
 */
@Component
public class FlexiJobBeToolUsageContributor
        implements ToolUsageContributor {

    private final FlexiJobBeAnalysisRepository repository;

    public FlexiJobBeToolUsageContributor(
            FlexiJobBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return FlexiJobBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        FlexiJobBeToolBranchRegistry.TOOL_ID,
                        FlexiJobBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
