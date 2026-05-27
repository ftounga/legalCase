package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-IM-28-naturalisation-12bis-be (F-215 P2 Immigration BE / SF-215-07). */
@Component
public class Naturalisation12bisBeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-IM-28-naturalisation-12bis-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
