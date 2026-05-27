package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil rcc-be-longue-carriere (BELGIQUE)
 * via la présence d'une {@link RccBeLongueCarriereAnalysis} pour le
 * {@code caseFileId} considéré. Permet à la mapping jurisprudence de
 * remonter les arrêts pertinents (Cour du travail BE, Cassation, ONEM
 * sur le RCC longue carrière, conditions âge / carrière, indemnité
 * complémentaire) dans la synthèse de dossier.
 */
@Component
public class RccBeLongueCarriereToolUsageContributor implements ToolUsageContributor {

    private final RccBeLongueCarriereAnalysisRepository repository;

    public RccBeLongueCarriereToolUsageContributor(
            RccBeLongueCarriereAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RccBeLongueCarriereToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RccBeLongueCarriereToolBranchRegistry.TOOL_ID,
                        RccBeLongueCarriereToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
