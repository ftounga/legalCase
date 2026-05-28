package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-25 — branches valides de l'outil
 * {@code auditorat-travail-be} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique d'orientation de saisine (SAISINE_AUDITORAT_
 * RECOMMANDEE / DENONCIATION_INSPECTION_PREALABLE /
 * SAISINE_NON_PERTINENTE / A_QUALIFIER), la base juridique commune
 * (art. 138bis C. jud. + art. 24 CIC + Loi 03/08/1992) ne justifie
 * pas une segmentation jurisprudence par verdict / mode de saisine.
 */
@Component
public class AuditoratTravailBeToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "auditorat-travail-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
