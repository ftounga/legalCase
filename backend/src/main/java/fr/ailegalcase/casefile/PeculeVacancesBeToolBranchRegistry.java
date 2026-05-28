package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-20 — branches valides de l'outil
 * {@code pecule-vacances-be} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique de calcul du pécule (simple / double / départ),
 * la base juridique commune (Lois coord. 28/06/1971 + AR 30/03/1967)
 * ne justifie pas une segmentation jurisprudence par type de pécule.
 */
@Component
public class PeculeVacancesBeToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "pecule-vacances-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
