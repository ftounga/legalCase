package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-223-05 — regime-algerien-be (Famille BELGIQUE). */
@Component
public class RegimeAlgerienBeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "regime-algerien-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
