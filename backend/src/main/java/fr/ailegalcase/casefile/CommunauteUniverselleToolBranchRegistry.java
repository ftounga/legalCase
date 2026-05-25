package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-99e v3 — branches valides de F-FA-16-communaute-universelle (V1 = default). */
@Component
public class CommunauteUniverselleToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "F-FA-16-communaute-universelle";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
