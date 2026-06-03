package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-223-08 — dip-be-reconnaissance-decision-etrangere (Famille BELGIQUE). */
@Component
public class DipBeReconnaissanceDecisionEtrangereToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "dip-be-reconnaissance-decision-etrangere";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
