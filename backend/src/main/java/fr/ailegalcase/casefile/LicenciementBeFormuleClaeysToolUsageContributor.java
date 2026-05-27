package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil licenciement-be-formule-claeys
 * (BELGIQUE) via la présence d'une
 * {@link LicenciementBeFormuleClaeysAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (notamment Cass. BE sur la formule Claeys et la clause de
 * sauvegarde art. 67 loi 26/12/2013) dans la synthèse de dossier.
 */
@Component
public class LicenciementBeFormuleClaeysToolUsageContributor implements ToolUsageContributor {

    private final LicenciementBeFormuleClaeysAnalysisRepository repository;

    public LicenciementBeFormuleClaeysToolUsageContributor(
            LicenciementBeFormuleClaeysAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LicenciementBeFormuleClaeysToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        LicenciementBeFormuleClaeysToolBranchRegistry.TOOL_ID,
                        LicenciementBeFormuleClaeysToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
