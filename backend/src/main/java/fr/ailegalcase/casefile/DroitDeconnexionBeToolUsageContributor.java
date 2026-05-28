package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-19 — détecte l'usage de l'outil
 * {@code droit-deconnexion-be} (BELGIQUE) via la présence d'une
 * {@link DroitDeconnexionBeAnalysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (jurisprudence post-2023 sur l'obligation art. 16,
 * action en cessation, contentieux pénal C. pén. social art. 137) dans
 * la synthèse de dossier.
 */
@Component
public class DroitDeconnexionBeToolUsageContributor
        implements ToolUsageContributor {

    private final DroitDeconnexionBeAnalysisRepository repository;

    public DroitDeconnexionBeToolUsageContributor(
            DroitDeconnexionBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DroitDeconnexionBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        DroitDeconnexionBeToolBranchRegistry.TOOL_ID,
                        DroitDeconnexionBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
