package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil licenciement-be-fermeture-entreprise
 * (BELGIQUE) via la présence d'une {@link
 * LicenciementBeFermetureEntrepriseAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (Cour du travail BE, Cassation, ONEM-FFE sur l'éligibilité
 * d'une fermeture, calcul de l'indemnité, reprise des créances) dans la
 * synthèse de dossier.
 */
@Component
public class LicenciementBeFermetureEntrepriseToolUsageContributor
        implements ToolUsageContributor {

    private final LicenciementBeFermetureEntrepriseAnalysisRepository repository;

    public LicenciementBeFermetureEntrepriseToolUsageContributor(
            LicenciementBeFermetureEntrepriseAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LicenciementBeFermetureEntrepriseToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        LicenciementBeFermetureEntrepriseToolBranchRegistry.TOOL_ID,
                        LicenciementBeFermetureEntrepriseToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
