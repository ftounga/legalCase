package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-23 — branches valides de l'outil
 * {@code discrimination-be-handicap-amenagement} (BELGIQUE) pour le
 * mapping jurisprudence. V1 = single branch {@code default} :
 * l'outil produit un verdict unique de qualification du refus
 * d'aménagement raisonnable, la base juridique commune (Loi
 * 10/05/2007 art. 14) ne justifie pas une segmentation jurisprudence
 * par verdict.
 */
@Component
public class DiscriminationBeHandicapAmenagementToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "discrimination-be-handicap-amenagement";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
