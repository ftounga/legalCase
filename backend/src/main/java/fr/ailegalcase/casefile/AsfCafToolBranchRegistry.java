package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-222-01 — F-FA-ASF-CAF (allocation de soutien familial). */
@Component
public class AsfCafToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-FA-ASF-CAF";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
