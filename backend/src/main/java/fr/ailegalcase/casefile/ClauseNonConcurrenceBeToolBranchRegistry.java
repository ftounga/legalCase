package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil clause-non-concurrence-be
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (l'outil n'a pas de sous-régimes calculatoires distincts
 * — la même formule s'applique à toutes les clauses valides).
 */
@Component
public class ClauseNonConcurrenceBeToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "clause-non-concurrence-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
