package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-14 — branches valides de l'outil
 * {@code interim-be-cct-322} (BELGIQUE) pour le mapping jurisprudence.
 * V1 = single branch {@code default} : l'outil produit un verdict
 * unique de validité de la mission, la base juridique commune
 * (Loi 24/07/1987 + CCT n° 322 du 14/06/2010) ne justifie pas une
 * segmentation jurisprudence par verdict ou par motif.
 */
@Component
public class InterimBeCct322ToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "interim-be-cct-322";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
