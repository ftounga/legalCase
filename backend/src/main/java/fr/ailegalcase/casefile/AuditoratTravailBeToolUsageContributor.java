package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-25 — détecte l'usage de l'outil
 * {@code auditorat-travail-be} (BELGIQUE) via la présence d'une
 * {@link AuditoratTravailBeAnalysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (Cass. ch. néerl. 20/03/2018 P.17.1175.N compétence
 * exclusive de l'auditeur du travail, Cour const. arrêt 61/2014 du
 * 03/04/2014 sur la « règle Una Via », CEDH Grande Stevens c. Italie
 * 04/03/2014 sur le non bis in idem) dans la synthèse de dossier.
 */
@Component
public class AuditoratTravailBeToolUsageContributor
        implements ToolUsageContributor {

    private final AuditoratTravailBeAnalysisRepository repository;

    public AuditoratTravailBeToolUsageContributor(
            AuditoratTravailBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return AuditoratTravailBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        AuditoratTravailBeToolBranchRegistry.TOOL_ID,
                        AuditoratTravailBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
