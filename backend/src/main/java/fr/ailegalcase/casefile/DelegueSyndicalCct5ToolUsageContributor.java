package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-10 — détecte l'usage de l'outil
 * {@code delegue-syndical-cct-5} (BELGIQUE) via la présence d'une
 * {@link DelegueSyndicalCct5Analysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (Cass. BE en matière de désignation, représentativité
 * syndicale, exercice des missions DS) dans la synthèse de dossier.
 */
@Component
public class DelegueSyndicalCct5ToolUsageContributor
        implements ToolUsageContributor {

    private final DelegueSyndicalCct5AnalysisRepository repository;

    public DelegueSyndicalCct5ToolUsageContributor(
            DelegueSyndicalCct5AnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return DelegueSyndicalCct5ToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        DelegueSyndicalCct5ToolBranchRegistry.TOOL_ID,
                        DelegueSyndicalCct5ToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
