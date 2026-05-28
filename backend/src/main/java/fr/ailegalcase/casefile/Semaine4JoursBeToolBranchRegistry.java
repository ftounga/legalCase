package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-18 — branches valides de l'outil
 * {@code semaine-4-jours-be} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique de conformité de la mise en place de la semaine
 * de 4 jours, la base juridique commune (Loi 03/10/2022 art. 5)
 * ne justifie pas une segmentation jurisprudence par verdict.
 */
@Component
public class Semaine4JoursBeToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "semaine-4-jours-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
