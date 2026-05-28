package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import fr.ailegalcase.jurisprudencemapping.ToolUsageContributor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * F-JU-03 / SF-219-24 — détecte l'usage de l'outil
 * {@code code-penal-social-be} (BELGIQUE) via la présence d'une
 * {@link CodePenalSocialBeAnalysis} pour le {@code caseFileId}
 * considéré. Permet au mapping jurisprudence de remonter les arrêts
 * pertinents (Cass. ch. néerl. 21/11/2017 P.17.0532.N qualification
 * travail non déclaré niveau 4, Cour const. arrêt 61/2014 du
 * 03/04/2014 sur la « règle Una Via », Cour const. arrêt 158/2009
 * sur la conformité légalité / proportionnalité du Code pénal
 * social) dans la synthèse de dossier.
 */
@Component
public class CodePenalSocialBeToolUsageContributor
        implements ToolUsageContributor {

    private final CodePenalSocialBeAnalysisRepository repository;

    public CodePenalSocialBeToolUsageContributor(
            CodePenalSocialBeAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public String toolId() {
        return CodePenalSocialBeToolBranchRegistry.TOOL_ID;
    }

    @Override
    public Optional<ToolUsage> detectUsage(UUID caseFileId) {
        if (caseFileId == null) {
            return Optional.empty();
        }
        return repository.findByCaseFileId(caseFileId)
                .map(x -> new ToolUsage(
                        CodePenalSocialBeToolBranchRegistry.TOOL_ID,
                        CodePenalSocialBeToolBranchRegistry.BRANCHE_DEFAULT));
    }
}
