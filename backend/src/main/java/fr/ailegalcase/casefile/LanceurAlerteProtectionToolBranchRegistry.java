package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-DT-61 protection du lanceur d'alerte (FR). */
@Component
public class LanceurAlerteProtectionToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-61-lanceur-alerte-protection";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
