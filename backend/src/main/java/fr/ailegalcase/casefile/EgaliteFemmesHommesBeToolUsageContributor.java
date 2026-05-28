package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-22 — détecte l'usage de l'outil
 * {@code egalite-femmes-hommes-be} (BELGIQUE) via la présence d'une
 * {@link EgaliteFemmesHommesBeAnalysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (jurisprudence CJUE Bilka / Cadman / Brunnhofer sur
 * justification objective des écarts, Cass. BE 22/01/2018
 * S.16.0094.F sur caractère contractuel de l'égalité de rémunération,
 * action en cessation IEFH, contentieux pénal C. pén. social
 * art. 195/1) dans la synthèse de dossier.
 */
@Component
public class EgaliteFemmesHommesBeToolUsageContributor
        implements ToolUsageContributor {

    private final EgaliteFemmesHommesBeAnalysisRepository repository;

    public EgaliteFemmesHommesBeToolUsageContributor(
            EgaliteFemmesHommesBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return EgaliteFemmesHommesBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        EgaliteFemmesHommesBeToolBranchRegistry.TOOL_ID,
                        EgaliteFemmesHommesBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
