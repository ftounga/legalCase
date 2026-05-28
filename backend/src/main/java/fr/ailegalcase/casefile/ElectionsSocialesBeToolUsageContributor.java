package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil elections-sociales-be
 * (BELGIQUE) via la présence d'une {@link
 * ElectionsSocialesBeAnalysis} pour le {@code caseFileId} considéré.
 * Permet à la mapping jurisprudence de remonter les arrêts
 * pertinents (Cour du travail BE, Cassation, contentieux UTE,
 * protection des candidats Loi 19/03/1991, sanction art. 32 Loi
 * 04/12/2007) dans la synthèse de dossier.
 */
@Component
public class ElectionsSocialesBeToolUsageContributor
        implements ToolUsageContributor {

    private final ElectionsSocialesBeAnalysisRepository repository;

    public ElectionsSocialesBeToolUsageContributor(
            ElectionsSocialesBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return ElectionsSocialesBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        ElectionsSocialesBeToolBranchRegistry.TOOL_ID,
                        ElectionsSocialesBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
