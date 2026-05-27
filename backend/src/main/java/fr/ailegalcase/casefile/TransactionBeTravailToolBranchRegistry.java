package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil transaction-be-travail (BELGIQUE)
 * pour le mapping jurisprudence. V1 = single branch {@code default}
 * (l'analyse de validité de la transaction produit une matrice de
 * verdict unique ; pas de sous-régimes calculatoires indépendants).
 */
@Component
public class TransactionBeTravailToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "transaction-be-travail";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
