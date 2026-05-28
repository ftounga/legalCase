package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-17 — détecte l'usage de l'outil
 * {@code clause-ecolage-be} (BELGIQUE) via la présence d'une
 * {@link ClauseEcolageBeAnalysis} pour le {@code caseFileId} considéré.
 * Permet au mapping jurisprudence de remonter les arrêts pertinents
 * (Cass. BE sur l'art. 22bis Loi 03/07/1978 — qualification "formation
 * spécifique" et opposabilité, Cass. BE sur le RMMMG CCT n° 43 du CNT)
 * dans la synthèse de dossier.
 */
@Component
public class ClauseEcolageBeToolUsageContributor
        implements ToolUsageContributor {

    private final ClauseEcolageBeAnalysisRepository repository;

    public ClauseEcolageBeToolUsageContributor(
            ClauseEcolageBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ClauseEcolageBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        ClauseEcolageBeToolBranchRegistry.TOOL_ID,
                        ClauseEcolageBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
