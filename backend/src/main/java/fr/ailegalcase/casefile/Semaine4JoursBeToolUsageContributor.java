package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-18 — détecte l'usage de l'outil
 * {@code semaine-4-jours-be} (BELGIQUE) via la présence d'une
 * {@link Semaine4JoursBeAnalysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (jurisprudence post-2023 du tribunal du travail sur le
 * refus motivé art. 5 § 5, contentieux SPF ETCS sur les limites
 * quotidiennes art. 5 § 3, action en protection contre le
 * licenciement représailles art. 5 § 6) dans la synthèse de dossier.
 */
@Component
public class Semaine4JoursBeToolUsageContributor
        implements ToolUsageContributor {

    private final Semaine4JoursBeAnalysisRepository repository;

    public Semaine4JoursBeToolUsageContributor(
            Semaine4JoursBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return Semaine4JoursBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        Semaine4JoursBeToolBranchRegistry.TOOL_ID,
                        Semaine4JoursBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
