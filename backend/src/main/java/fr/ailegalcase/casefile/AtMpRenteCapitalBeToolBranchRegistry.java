package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-29 - branches valides de l'outil
 * {@code at-mp-rente-capital-be} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique (capital forfaitaire IPP &lt; 19 / rente annuelle
 * IPP &gt;= 19 / inegligible / IPP non determine / a qualifier), la
 * base juridique commune (Loi 10/04/1971 art. 24 + Lois coordonnees
 * 03/06/1970 art. 35 + AR 21/12/1971 + AR 24/02/2005) ne justifie pas
 * une segmentation jurisprudence par verdict.
 */
@Component
public class AtMpRenteCapitalBeToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "at-mp-rente-capital-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
