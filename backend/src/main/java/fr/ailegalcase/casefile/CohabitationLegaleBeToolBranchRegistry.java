package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-223-01 — cohabitation-legale-be (Famille BELGIQUE). */
@Component
public class CohabitationLegaleBeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "cohabitation-legale-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
