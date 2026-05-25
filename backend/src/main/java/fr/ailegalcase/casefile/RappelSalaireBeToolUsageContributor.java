package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil rappel-salaire-be (BELGIQUE) via la
 * présence d'une {@link RappelSalaireBeAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (notamment Cass. BE sur la prescription 1 an post-rupture et le
 * taux d'intérêts 10 % de la Loi 12/04/1965) dans la synthèse de dossier.
 */
@Component
public class RappelSalaireBeToolUsageContributor implements ToolUsageContributor {

    private final RappelSalaireBeAnalysisRepository repository;

    public RappelSalaireBeToolUsageContributor(RappelSalaireBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RappelSalaireBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RappelSalaireBeToolBranchRegistry.TOOL_ID,
                        RappelSalaireBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
