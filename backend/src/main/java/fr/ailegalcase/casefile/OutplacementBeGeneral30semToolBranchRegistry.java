package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil outplacement-be-general-30sem
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (l'outil produit un verdict conforme / non-dû / non-conforme
 * + détail par condition — pas de sous-régimes calculatoires indépendants
 * justifiant une segmentation jurisprudence).
 */
@Component
public class OutplacementBeGeneral30semToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "outplacement-be-general-30sem";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
