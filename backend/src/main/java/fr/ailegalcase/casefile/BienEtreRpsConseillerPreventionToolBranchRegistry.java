package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-30 — branches valides de l'outil
 * {@code bien-etre-rps-conseiller-prevention} (BELGIQUE) pour le
 * mapping jurisprudence. V1 = single branch {@code default} : l'outil
 * produit un verdict unique de conformité procédurale (saisine
 * conforme / informelle en cours / formelle en cours / avis rendu
 * dans les délais / avis hors délai / formalités manquantes / pas de
 * conseiller / à qualifier), la base juridique commune (Loi 04/08/1996
 * art. 32sexies + AR 10/04/2014) ne justifie pas une segmentation
 * jurisprudence par verdict.
 */
@Component
public class BienEtreRpsConseillerPreventionToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "bien-etre-rps-conseiller-prevention";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
