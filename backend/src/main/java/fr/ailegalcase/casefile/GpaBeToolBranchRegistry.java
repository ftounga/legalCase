package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-223-04 — gpa-be-situation-contentieuse (Famille BELGIQUE). */
@Component
public class GpaBeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "gpa-be-situation-contentieuse";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
