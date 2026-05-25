package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-99f — branches valides de regime-mat-be-communaute-legale BE (V1 = default). */
@Component
public class RegimeCommunauteLegaleBeToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "regime-mat-be-communaute-legale";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
