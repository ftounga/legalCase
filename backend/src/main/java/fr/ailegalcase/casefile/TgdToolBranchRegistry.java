package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-222-02 — F-FA-TGD (téléphone grave danger — éligibilité). */
@Component
public class TgdToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-FA-TGD";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
