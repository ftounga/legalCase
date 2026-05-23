package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-01 — F-DT-36 licenciement faute grave/lourde. */
@Component
public class LicenciementFauteGraveLourdToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-36-licenciement-faute-grave-lourde";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
