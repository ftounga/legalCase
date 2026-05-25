package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-99e v4 — branches valides de F-FA-26-changement-etat-civil (V1 = default). */
@Component
public class ChangementEtatCivilToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "F-FA-26-changement-etat-civil";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
