package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-14 — détecte l'usage de l'outil
 * {@code interim-be-cct-322} (BELGIQUE) via la présence d'une
 * {@link InterimBeCct322Analysis} pour le {@code caseFileId} considéré.
 * Permet au mapping jurisprudence de remonter les arrêts pertinents
 * (Cass. BE en matière de requalification du contrat intérimaire en
 * CDI utilisateur, Cour const. BE sur la validité de l'art. 1bis
 * insertion, Cass. soc. sur la parité salariale art. 10 / CCT 322)
 * dans la synthèse de dossier.
 */
@Component
public class InterimBeCct322ToolUsageContributor
        implements ToolUsageContributor {

    private final InterimBeCct322AnalysisRepository repository;

    public InterimBeCct322ToolUsageContributor(
            InterimBeCct322AnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return InterimBeCct322ToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        InterimBeCct322ToolBranchRegistry.TOOL_ID,
                        InterimBeCct322ToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
