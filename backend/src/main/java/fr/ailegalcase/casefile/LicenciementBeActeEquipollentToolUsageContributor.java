package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil licenciement-be-acte-equivalent
 * (BELGIQUE) via la présence d'une {@link LicenciementBeActeEquipollentAnalysis}
 * pour le {@code caseFileId} considéré. Permet à la mapping jurisprudence
 * de remonter les arrêts pertinents (Cass. BE et Cour du travail sur l'acte
 * équipollent à rupture — Loi 03/07/1978 art. 20) dans la synthèse de
 * dossier.
 */
@Component
public class LicenciementBeActeEquipollentToolUsageContributor implements ToolUsageContributor {

    private final LicenciementBeActeEquipollentRepository repository;

    public LicenciementBeActeEquipollentToolUsageContributor(
            LicenciementBeActeEquipollentRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return LicenciementBeActeEquipollentToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        LicenciementBeActeEquipollentToolBranchRegistry.TOOL_ID,
                        LicenciementBeActeEquipollentToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
