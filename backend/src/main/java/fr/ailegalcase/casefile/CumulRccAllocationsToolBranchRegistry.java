package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil cumul-rcc-allocations (BELGIQUE)
 * pour le mapping jurisprudence. V1 = single branch {@code default}
 * (l'outil produit un verdict de cumul + indicateurs de conformité — pas
 * de sous-régimes calculatoires indépendants justifiant une segmentation
 * jurisprudence séparée par variante).
 */
@Component
public class CumulRccAllocationsToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "cumul-rcc-allocations";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
