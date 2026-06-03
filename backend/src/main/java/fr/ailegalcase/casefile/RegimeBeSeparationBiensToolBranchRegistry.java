package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-223-06 — regime-be-separation-biens (Famille BELGIQUE). */
@Component
public class RegimeBeSeparationBiensToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "regime-be-separation-biens";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
