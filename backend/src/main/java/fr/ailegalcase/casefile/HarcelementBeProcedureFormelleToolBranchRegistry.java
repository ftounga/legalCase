package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil harcelement-be-procedure-formelle
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (la procédure interne produit une checklist contextuelle
 * unique ; pas de sous-régimes calculatoires indépendants).
 */
@Component
public class HarcelementBeProcedureFormelleToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "harcelement-be-procedure-formelle";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
