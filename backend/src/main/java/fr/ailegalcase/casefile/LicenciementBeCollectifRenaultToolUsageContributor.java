package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil licenciement-be-collectif-renault
 * (BELGIQUE) via la présence d'une {@link
 * LicenciementBeCollectifRenaultAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (Cour du travail BE, Cassation, ONEM sur le respect de la
 * procédure Renault, nullité des préavis pour violation du délai
 * d'attente, indemnité spéciale art. 67) dans la synthèse de dossier.
 */
@Component
public class LicenciementBeCollectifRenaultToolUsageContributor
        implements ToolUsageContributor {

    private final LicenciementBeCollectifRenaultAnalysisRepository repository;

    public LicenciementBeCollectifRenaultToolUsageContributor(
            LicenciementBeCollectifRenaultAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LicenciementBeCollectifRenaultToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        LicenciementBeCollectifRenaultToolBranchRegistry.TOOL_ID,
                        LicenciementBeCollectifRenaultToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
