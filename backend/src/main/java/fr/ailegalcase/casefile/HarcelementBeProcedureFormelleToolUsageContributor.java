package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil harcelement-be-procedure-formelle
 * (BELGIQUE) via la présence d'une
 * {@link HarcelementBeProcedureFormelleAnalysis} pour le {@code caseFileId}
 * considéré. Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (Cour du travail BE et Cassation sur la procédure interne
 * harcèlement, l'enquête CPAP, la protection contre représailles) dans la
 * synthèse de dossier.
 */
@Component
public class HarcelementBeProcedureFormelleToolUsageContributor implements ToolUsageContributor {

    private final HarcelementBeProcedureFormelleAnalysisRepository repository;

    public HarcelementBeProcedureFormelleToolUsageContributor(
            HarcelementBeProcedureFormelleAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return HarcelementBeProcedureFormelleToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        HarcelementBeProcedureFormelleToolBranchRegistry.TOOL_ID,
                        HarcelementBeProcedureFormelleToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
