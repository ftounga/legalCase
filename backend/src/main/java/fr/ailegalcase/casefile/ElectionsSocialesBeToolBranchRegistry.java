package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil elections-sociales-be
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (l'outil produit un verdict d'obligation /
 * calendrier ; les sous-verdicts ne justifient pas une segmentation
 * jurisprudence séparée car la base juridique reste la même — Loi
 * 04/12/2007 + AR 25/05/2012 + Loi 04/08/1996 + Loi 19/03/1991).
 */
@Component
public class ElectionsSocialesBeToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "elections-sociales-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
