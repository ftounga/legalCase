package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-05 — F-FA-10-divorce-accepte. */
@Component
public class DivorceAccepteToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-FA-10-divorce-accepte";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
