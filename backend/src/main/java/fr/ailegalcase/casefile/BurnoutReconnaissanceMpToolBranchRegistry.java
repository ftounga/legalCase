package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-DT-64 burn-out reconnaissance MP (FR). */
@Component
public class BurnoutReconnaissanceMpToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-64-burnout-reconnaissance-mp";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
