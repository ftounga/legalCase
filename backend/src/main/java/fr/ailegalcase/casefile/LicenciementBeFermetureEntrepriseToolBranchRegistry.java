package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil licenciement-be-fermeture-entreprise
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch {@code default}
 * (l'outil produit un verdict d'éligibilité + calcul d'indemnité ; les
 * sous-régimes FFE/indemnité seule/inéligibilité ne justifient pas une
 * segmentation jurisprudence séparée car la base juridique reste la même —
 * Loi 26/06/2002 + AR 23/03/2007).
 */
@Component
public class LicenciementBeFermetureEntrepriseToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "licenciement-be-fermeture-entreprise";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
