package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-02-04-06 — F-FA-11-desunion-irremediable-be. */
@Component
public class DivorceDesunionIrremediableBeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-FA-11-desunion-irremediable-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
