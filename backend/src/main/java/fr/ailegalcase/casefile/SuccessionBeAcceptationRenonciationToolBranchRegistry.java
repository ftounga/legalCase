package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-99f — branches valides de succession-be-acceptation-renonciation BE (V1 = default). */
@Component
public class SuccessionBeAcceptationRenonciationToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "succession-be-acceptation-renonciation";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
