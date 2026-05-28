package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-24 — branches valides de l'outil
 * {@code code-penal-social-be} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique de qualification d'infraction sociale et de
 * niveau de sanction (1 à 4 ou A_QUALIFIER), la base juridique
 * commune (Loi du 06/06/2010 art. 101-110) ne justifie pas une
 * segmentation jurisprudence par verdict / niveau.
 */
@Component
public class CodePenalSocialBeToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "code-penal-social-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
