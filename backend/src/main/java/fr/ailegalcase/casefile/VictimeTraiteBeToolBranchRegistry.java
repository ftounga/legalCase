package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-IM-58-victime-traite-be (F-221 P3 Immigration BE / SF-221-06). */
@Component
public class VictimeTraiteBeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-IM-58-victime-traite-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
