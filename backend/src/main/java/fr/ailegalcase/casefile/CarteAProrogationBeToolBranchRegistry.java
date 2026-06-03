package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-IM-53-carte-a-prorogation-be (F-221 P3 Immigration BE / SF-221-01). */
@Component
public class CarteAProrogationBeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-IM-53-carte-a-prorogation-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
