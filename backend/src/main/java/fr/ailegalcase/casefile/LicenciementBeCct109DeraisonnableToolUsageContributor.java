package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil licenciement-be-cct109-deraisonnable
 * (BELGIQUE) via la présence d'une
 * {@link LicenciementBeCct109DeraisonnableAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (Cour du travail BE et Cassation sur la CCT 109, l'art. 9 et
 * la qualification du licenciement manifestement déraisonnable) dans la
 * synthèse de dossier.
 */
@Component
public class LicenciementBeCct109DeraisonnableToolUsageContributor implements ToolUsageContributor {

    private final LicenciementBeCct109DeraisonnableAnalysisRepository repository;

    public LicenciementBeCct109DeraisonnableToolUsageContributor(
            LicenciementBeCct109DeraisonnableAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LicenciementBeCct109DeraisonnableToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        LicenciementBeCct109DeraisonnableToolBranchRegistry.TOOL_ID,
                        LicenciementBeCct109DeraisonnableToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
