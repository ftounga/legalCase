package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil licenciement-be-statut-unique-preavis
 * (BELGIQUE) via la présence d'une
 * {@link LicenciementBeStatutUniquePreavisAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (notamment Cass. BE sur la loi du 26/12/2013 et le barème
 * statut unique) dans la synthèse de dossier.
 */
@Component
public class LicenciementBeStatutUniquePreavisToolUsageContributor implements ToolUsageContributor {

    private final LicenciementBeStatutUniquePreavisAnalysisRepository repository;

    public LicenciementBeStatutUniquePreavisToolUsageContributor(
            LicenciementBeStatutUniquePreavisAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LicenciementBeStatutUniquePreavisToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        LicenciementBeStatutUniquePreavisToolBranchRegistry.TOOL_ID,
                        LicenciementBeStatutUniquePreavisToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
