package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-19 — branches valides de l'outil
 * {@code droit-deconnexion-be} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique de conformité de l'obligation déconnexion, la
 * base juridique commune (Loi 03/10/2022 art. 16) ne justifie pas une
 * segmentation jurisprudence par verdict.
 */
@Component
public class DroitDeconnexionBeToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "droit-deconnexion-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
