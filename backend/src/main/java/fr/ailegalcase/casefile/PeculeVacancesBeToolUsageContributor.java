package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-20 — détecte l'usage de l'outil
 * {@code pecule-vacances-be} (BELGIQUE) via la présence d'une
 * {@link PeculeVacancesBeAnalysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (jurisprudence Cour de cassation et tribunaux du travail
 * sur le calcul de la base de pécule art. 19 / 38 / 46 AR 30/03/1967,
 * contentieux ONVA sur le défaut de cotisation employeur, prescription
 * art. 15 Loi 03/07/1978) dans la synthèse de dossier.
 */
@Component
public class PeculeVacancesBeToolUsageContributor
        implements ToolUsageContributor {

    private final PeculeVacancesBeAnalysisRepository repository;

    public PeculeVacancesBeToolUsageContributor(
            PeculeVacancesBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return PeculeVacancesBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        PeculeVacancesBeToolBranchRegistry.TOOL_ID,
                        PeculeVacancesBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
