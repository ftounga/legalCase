package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-01 — F-DT-22 requalification CDD en CDI. */
@Component
public class RequalificationCddCdiToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-22-requalification-cdd-cdi";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
