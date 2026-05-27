package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil licenciement-be-protection-deleguee
 * (BELGIQUE) via la présence d'une
 * {@link LicenciementBeProtectionDelegueeAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (Cour du travail BE et Cassation sur la protection des
 * représentants des travailleurs, l'indemnité forfaitaire 2-4 ans, la
 * procédure de réintégration) dans la synthèse de dossier.
 */
@Component
public class LicenciementBeProtectionDelegueeToolUsageContributor implements ToolUsageContributor {

    private final LicenciementBeProtectionDelegueeAnalysisRepository repository;

    public LicenciementBeProtectionDelegueeToolUsageContributor(
            LicenciementBeProtectionDelegueeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LicenciementBeProtectionDelegueeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        LicenciementBeProtectionDelegueeToolBranchRegistry.TOOL_ID,
                        LicenciementBeProtectionDelegueeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
