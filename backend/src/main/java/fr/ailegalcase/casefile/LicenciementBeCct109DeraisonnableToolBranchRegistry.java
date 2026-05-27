package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil licenciement-be-cct109-deraisonnable
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (l'outil produit un score discret 0/3/8/12/17 — pas de
 * sous-régimes calculatoires indépendants justifiant une segmentation
 * jurisprudence ; les arrêts CT/Cassation BE sur la CCT 109 portent
 * uniformément sur la motivation du licenciement).
 */
@Component
public class LicenciementBeCct109DeraisonnableToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "licenciement-be-cct109-deraisonnable";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
