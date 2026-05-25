package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil clause-non-concurrence-be (BELGIQUE)
 * via la présence d'une {@link ClauseNonConcurrenceBeAnalysis} pour le
 * {@code caseFileId} considéré. Permet à la mapping jurisprudence de
 * remonter les arrêts pertinents (notamment Cass. BE sur l'art. 65 et la
 * CCT 13) dans la synthèse de dossier.
 */
@Component
public class ClauseNonConcurrenceBeToolUsageContributor implements ToolUsageContributor {

    private final ClauseNonConcurrenceBeAnalysisRepository repository;

    public ClauseNonConcurrenceBeToolUsageContributor(ClauseNonConcurrenceBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ClauseNonConcurrenceBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        ClauseNonConcurrenceBeToolBranchRegistry.TOOL_ID,
                        ClauseNonConcurrenceBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
