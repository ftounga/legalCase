package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil rcc-be-entreprise-difficulte
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (l'outil produit un verdict éligible / inéligible + 5
 * raisons cumulatives — pas de sous-régimes calculatoires indépendants
 * justifiant une segmentation jurisprudence).
 */
@Component
public class RccBeEntrepriseDifficulteToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "rcc-be-entreprise-difficulte";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
