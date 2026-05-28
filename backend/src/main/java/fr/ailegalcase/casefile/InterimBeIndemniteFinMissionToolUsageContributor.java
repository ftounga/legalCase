package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-15 — détecte l'usage de l'outil
 * {@code interim-be-indemnite-fin-mission} (BELGIQUE) via la présence
 * d'une {@link InterimBeIndemniteFinMissionAnalysis} pour le
 * {@code caseFileId} considéré.
 *
 * <p>Permet au mapping jurisprudence de remonter les arrêts pertinents
 * (Cass. BE 04/02/1991 + 16/12/2002 sur la rupture anticipée d'une
 * mission intérim, Cass. BE 03/05/2010 + 23/06/2003 sur le refus de
 * la prime de précarité 10 % style FR, jurisprudence du fond sur la
 * parité salariale art. 10 et les CCT sectorielles) dans la synthèse
 * de dossier.</p>
 */
@Component
public class InterimBeIndemniteFinMissionToolUsageContributor
        implements ToolUsageContributor {

    private final InterimBeIndemniteFinMissionAnalysisRepository repository;

    public InterimBeIndemniteFinMissionToolUsageContributor(
            InterimBeIndemniteFinMissionAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return InterimBeIndemniteFinMissionToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        InterimBeIndemniteFinMissionToolBranchRegistry.TOOL_ID,
                        InterimBeIndemniteFinMissionToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
