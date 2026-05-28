package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-11 — détecte l'usage de l'outil
 * {@code conge-education-paye-region} (BELGIQUE) via la présence
 * d'une {@link CongeEducationPayeRegionAnalysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (Cass. BE en matière de congé-éducation payé,
 * contentieux d'éligibilité, recours administratifs régionaux) dans
 * la synthèse de dossier.
 */
@Component
public class CongeEducationPayeRegionToolUsageContributor
        implements ToolUsageContributor {

    private final CongeEducationPayeRegionAnalysisRepository repository;

    public CongeEducationPayeRegionToolUsageContributor(
            CongeEducationPayeRegionAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CongeEducationPayeRegionToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        CongeEducationPayeRegionToolBranchRegistry.TOOL_ID,
                        CongeEducationPayeRegionToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
