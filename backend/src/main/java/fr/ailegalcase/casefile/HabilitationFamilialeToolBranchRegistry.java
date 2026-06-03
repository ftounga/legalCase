package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-222-03 — F-FA-HABILITATION-FAMILIALE (habilitation familiale, art. 494-1 et s. Cciv). */
@Component
public class HabilitationFamilialeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-FA-HABILITATION-FAMILIALE";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
