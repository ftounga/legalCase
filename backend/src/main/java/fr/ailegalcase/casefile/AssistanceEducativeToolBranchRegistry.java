package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-222-04 — F-FA-ASSISTANCE-EDUCATIVE (assistance éducative, art. 375 et s. Cciv). */
@Component
public class AssistanceEducativeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-FA-ASSISTANCE-EDUCATIVE";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
