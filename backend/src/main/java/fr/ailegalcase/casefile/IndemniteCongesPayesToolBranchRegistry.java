package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-01 — F-DT-26 indemnité congés payés. */
@Component
public class IndemniteCongesPayesToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-26-conges-payes-indemnite";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
