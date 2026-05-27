package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 — détecte l'usage de l'outil rcc-be-metiers-lourds (BELGIQUE)
 * via la présence d'une {@link RccBeMetiersLourdsAnalysis} pour le
 * {@code caseFileId} considéré. Permet au mapping jurisprudence de
 * remonter les arrêts pertinents (Cour du travail BE et Cassation sur
 * le RCC, l'AR 03/05/2007, la notion de métier lourd / pénibilité) dans
 * la synthèse de dossier.
 */
@Component
public class RccBeMetiersLourdsToolUsageContributor implements ToolUsageContributor {

    private final RccBeMetiersLourdsAnalysisRepository repository;

    public RccBeMetiersLourdsToolUsageContributor(
            RccBeMetiersLourdsAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return RccBeMetiersLourdsToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        RccBeMetiersLourdsToolBranchRegistry.TOOL_ID,
                        RccBeMetiersLourdsToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
