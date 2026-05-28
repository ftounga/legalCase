package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-30 — détecte l'usage de l'outil
 * {@code bien-etre-rps-conseiller-prevention} (BELGIQUE) via la
 * présence d'une {@link BienEtreRpsConseillerPreventionAnalysis} pour
 * le {@code caseFileId} considéré. Permet au mapping jurisprudence de
 * remonter les arrêts pertinents (Cass. BE 23/12/2014 S.14.0026.N
 * formalisme strict de la demande formelle, Cass. BE 20/01/2014
 * S.12.0064.F déclenchement protection 12 mois art. 32sexies par la
 * demande formelle) dans la synthèse de dossier.
 */
@Component
public class BienEtreRpsConseillerPreventionToolUsageContributor
        implements ToolUsageContributor {

    private final BienEtreRpsConseillerPreventionAnalysisRepository repository;

    public BienEtreRpsConseillerPreventionToolUsageContributor(
            BienEtreRpsConseillerPreventionAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return BienEtreRpsConseillerPreventionToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        BienEtreRpsConseillerPreventionToolBranchRegistry.TOOL_ID,
                        BienEtreRpsConseillerPreventionToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
