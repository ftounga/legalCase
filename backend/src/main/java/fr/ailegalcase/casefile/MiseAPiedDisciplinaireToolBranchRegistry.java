package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-DT-48 mise à pied disciplinaire — régularité (FR). */
@Component
public class MiseAPiedDisciplinaireToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-48-mise-a-pied-disciplinaire";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
