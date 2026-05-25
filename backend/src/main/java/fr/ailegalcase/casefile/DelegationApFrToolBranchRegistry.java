package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-99e — branches valides de F-FA-XX-delegation-ap (V1 = default). */
@Component
public class DelegationApFrToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "F-FA-XX-delegation-ap";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
