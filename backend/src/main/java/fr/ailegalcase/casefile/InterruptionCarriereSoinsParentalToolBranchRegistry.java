package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-32 — branches valides de l'outil
 * {@code interruption-carriere-soins-parental} (BELGIQUE) pour le
 * mapping jurisprudence. V1 = single branch {@code default} : l'outil
 * produit un verdict unique d'éligibilité au congé parental (éligible
 * complet / éligible avec réserves / inéligible ancienneté / inéligible
 * âge enfant / inéligible solde / inéligible formalisme / différé
 * employeur / à qualifier), la base juridique commune (Loi 22/01/1985
 * art. 99 à 107quater + AR 29/10/1997 + CCT 64) ne justifie pas une
 * segmentation jurisprudence par verdict.
 */
@Component
public class InterruptionCarriereSoinsParentalToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "interruption-carriere-soins-parental";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
