package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil cumul-rcc-allocations (BELGIQUE)
 * via la présence d'une {@link CumulRccAllocationsAnalysis} pour le
 * {@code caseFileId} considéré. Permet à la mapping jurisprudence de
 * remonter les arrêts pertinents (Cour du travail BE, Cassation, ONEM
 * sur le plafond du cumul, disponibilité ajustée, sortie du régime
 * pour activité non déclarée) dans la synthèse de dossier.
 */
@Component
public class CumulRccAllocationsToolUsageContributor implements ToolUsageContributor {

    private final CumulRccAllocationsAnalysisRepository repository;

    public CumulRccAllocationsToolUsageContributor(
            CumulRccAllocationsAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CumulRccAllocationsToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        CumulRccAllocationsToolBranchRegistry.TOOL_ID,
                        CumulRccAllocationsToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
