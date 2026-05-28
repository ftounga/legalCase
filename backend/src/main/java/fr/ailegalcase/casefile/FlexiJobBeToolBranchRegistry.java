package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-12 — branches valides de l'outil
 * {@code flexi-job-be} (BELGIQUE) pour le mapping jurisprudence.
 * V1 = single branch {@code default} : l'outil produit un verdict
 * d'éligibilité unique au régime flexi-job, la base juridique
 * commune (Loi-programme 26/12/2013 + extensions successives) ne
 * justifie pas une segmentation jurisprudence par verdict ou par
 * secteur.
 */
@Component
public class FlexiJobBeToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "flexi-job-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
