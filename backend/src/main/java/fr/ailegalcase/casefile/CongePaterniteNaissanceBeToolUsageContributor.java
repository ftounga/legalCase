package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-31 — détecte l'usage de l'outil
 * {@code conge-paternite-naissance-be} (BELGIQUE) via la présence d'une
 * {@link CongePaterniteNaissanceBeAnalysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (Cass. BE 16/03/2015 S.13.0094.F protection 5 mois
 * art. 30 § 4, Cass. BE 25/11/2019 S.18.0086.F preuve filiation,
 * Cass. BE 12/05/2014 S.13.0103.F articulation suspensions) dans la
 * synthèse de dossier.
 */
@Component
public class CongePaterniteNaissanceBeToolUsageContributor
        implements ToolUsageContributor {

    private final CongePaterniteNaissanceBeAnalysisRepository repository;

    public CongePaterniteNaissanceBeToolUsageContributor(
            CongePaterniteNaissanceBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CongePaterniteNaissanceBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        CongePaterniteNaissanceBeToolBranchRegistry.TOOL_ID,
                        CongePaterniteNaissanceBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
