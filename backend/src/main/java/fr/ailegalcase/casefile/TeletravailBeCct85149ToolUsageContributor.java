package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-16 — détecte l'usage de l'outil
 * {@code teletravail-be-cct-85-149} (BELGIQUE) via la présence d'une
 * {@link TeletravailBeCct85149Analysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (Cass. BE sur la convention écrite art. 6 CCT n° 85,
 * Cour const. BE sur l'égalité de traitement art. 4, contentieux SPF
 * ETCS sur les modalités de déconnexion Loi 03/10/2022) dans la
 * synthèse de dossier.
 */
@Component
public class TeletravailBeCct85149ToolUsageContributor
        implements ToolUsageContributor {

    private final TeletravailBeCct85149AnalysisRepository repository;

    public TeletravailBeCct85149ToolUsageContributor(
            TeletravailBeCct85149AnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return TeletravailBeCct85149ToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        TeletravailBeCct85149ToolBranchRegistry.TOOL_ID,
                        TeletravailBeCct85149ToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
