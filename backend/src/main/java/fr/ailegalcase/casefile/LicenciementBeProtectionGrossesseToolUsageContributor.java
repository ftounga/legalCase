package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil licenciement-be-protection-grossesse
 * (BELGIQUE) via la présence d'une
 * {@link LicenciementBeProtectionGrossesseAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (notamment Cass. BE et Cour du travail sur la protection
 * grossesse / maternité art. 40 Loi 16/03/1971 et la fenêtre de
 * présomption des 10 semaines) dans la synthèse de dossier.
 */
@Component
public class LicenciementBeProtectionGrossesseToolUsageContributor implements ToolUsageContributor {

    private final LicenciementBeProtectionGrossesseAnalysisRepository repository;

    public LicenciementBeProtectionGrossesseToolUsageContributor(
            LicenciementBeProtectionGrossesseAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LicenciementBeProtectionGrossesseToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        LicenciementBeProtectionGrossesseToolBranchRegistry.TOOL_ID,
                        LicenciementBeProtectionGrossesseToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
