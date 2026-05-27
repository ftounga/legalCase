package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-IM-25-single-permit-be (F-215 P2 Immigration BE / SF-215-01). */
@Component
public class SinglePermitBeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-IM-25-single-permit-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
