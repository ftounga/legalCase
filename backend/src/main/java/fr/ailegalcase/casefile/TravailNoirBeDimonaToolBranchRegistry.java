package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-26 — branches valides de l'outil
 * {@code travail-noir-be-dimona} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique (DIMONA conforme / tardive / absente / présomption
 * salariat / faux indépendant / à qualifier), la base juridique
 * commune (Loi-programme 24/12/2002 + AR 05/11/2002 + Code pénal
 * social art. 181) ne justifie pas une segmentation jurisprudence
 * par verdict.
 */
@Component
public class TravailNoirBeDimonaToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "travail-noir-be-dimona";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
