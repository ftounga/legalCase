package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-DT-71 mutation — validité de la clause de mobilité (FR). */
@Component
public class MutationClauseMobiliteToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-71-mutation-clause-mobilite";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
