package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil rcc-be-entreprise-difficulte
 * (BELGIQUE) via la présence d'une
 * {@link RccBeEntrepriseDifficulteAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (Cour du travail BE, Cassation, ONEM sur le RCC entreprise
 * en difficulté / restructuration, reconnaissance ministre, indemnité
 * complémentaire) dans la synthèse de dossier.
 */
@Component
public class RccBeEntrepriseDifficulteToolUsageContributor
        implements ToolUsageContributor {

    private final RccBeEntrepriseDifficulteAnalysisRepository repository;

    public RccBeEntrepriseDifficulteToolUsageContributor(
            RccBeEntrepriseDifficulteAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RccBeEntrepriseDifficulteToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RccBeEntrepriseDifficulteToolBranchRegistry.TOOL_ID,
                        RccBeEntrepriseDifficulteToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
