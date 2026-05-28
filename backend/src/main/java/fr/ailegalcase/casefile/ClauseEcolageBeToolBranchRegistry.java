package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-17 — branches valides de l'outil
 * {@code clause-ecolage-be} (BELGIQUE) pour le mapping jurisprudence.
 * V1 = single branch {@code default} : l'outil produit un verdict
 * unique de validité de la clause d'écolage et un montant dégressif
 * unique, la base juridique commune (art. 22bis Loi 03/07/1978 + CCT
 * n° 43 + CCT n° 13) ne justifie pas une segmentation jurisprudence
 * par verdict.
 */
@Component
public class ClauseEcolageBeToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "clause-ecolage-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
