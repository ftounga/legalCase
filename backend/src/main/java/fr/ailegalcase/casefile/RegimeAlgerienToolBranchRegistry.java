package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-99c v2 — branches valides de F-IM-17-regime-algerien (V1 = default). */
@Component
public class RegimeAlgerienToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "F-IM-17-regime-algerien";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
