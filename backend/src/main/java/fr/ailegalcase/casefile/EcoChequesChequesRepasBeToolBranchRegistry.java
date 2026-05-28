package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-21 — branches valides de l'outil
 * {@code eco-cheques-cheques-repas-be} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique d'analyse de conformité, la base juridique commune
 * (CCT n°98 + Loi 25/04/2014 + AR 03/02/2010 + jurisprudence ONSS) ne
 * justifie pas une segmentation jurisprudence par type d'avantage.
 */
@Component
public class EcoChequesChequesRepasBeToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "eco-cheques-cheques-repas-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
