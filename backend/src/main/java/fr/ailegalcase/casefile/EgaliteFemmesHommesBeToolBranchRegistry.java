package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-22 — branches valides de l'outil
 * {@code egalite-femmes-hommes-be} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique de conformité de l'obligation rapport biennal
 * égalité H/F, la base juridique commune (Loi 22/04/2012 art. 2)
 * ne justifie pas une segmentation jurisprudence par verdict.
 */
@Component
public class EgaliteFemmesHommesBeToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "egalite-femmes-hommes-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
