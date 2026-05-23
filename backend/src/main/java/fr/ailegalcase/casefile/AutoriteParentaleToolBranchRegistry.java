package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-05 — F-FA-19-autorite-parentale. */
@Component
public class AutoriteParentaleToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-FA-19-autorite-parentale";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
