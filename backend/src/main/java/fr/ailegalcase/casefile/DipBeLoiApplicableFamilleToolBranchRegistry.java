package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-223-07 — dip-be-loi-applicable-famille (Famille BELGIQUE). */
@Component
public class DipBeLoiApplicableFamilleToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "dip-be-loi-applicable-famille";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
