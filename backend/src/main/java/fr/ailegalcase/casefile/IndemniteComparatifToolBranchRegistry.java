package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-JU-03-01 — déclare la branche du F-DT-09 comparateur indemnités
 * pour le cron dérive (SF-JU-01-03).
 */
@Component
public class IndemniteComparatifToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-09-comparateur-indemnites";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
