package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-32 — détecte l'usage de l'outil
 * {@code interruption-carriere-soins-parental} (BELGIQUE) via la
 * présence d'une {@link InterruptionCarriereSoinsParentalAnalysis}
 * pour le {@code caseFileId} considéré. Permet au mapping jurisprudence
 * de remonter les arrêts pertinents (Cass. BE 26/05/2008 S.07.0040.F
 * protection licenciement art. 101 Loi 22/01/1985 opposable même si la
 * demande n'a pas été notifiée dans le strict délai dès lors qu'elle
 * a été acceptée ou consommée par l'employeur, Cass. BE 22/06/2009
 * S.08.0102.N motif grave et raisons étrangères pour la levée de la
 * protection, Cass. BE 16/01/2017 S.15.0102.N cumul allocation ONEM
 * de droit) dans la synthèse de dossier.
 */
@Component
public class InterruptionCarriereSoinsParentalToolUsageContributor
        implements ToolUsageContributor {

    private final InterruptionCarriereSoinsParentalAnalysisRepository repository;

    public InterruptionCarriereSoinsParentalToolUsageContributor(
            InterruptionCarriereSoinsParentalAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return InterruptionCarriereSoinsParentalToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        InterruptionCarriereSoinsParentalToolBranchRegistry.TOOL_ID,
                        InterruptionCarriereSoinsParentalToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
