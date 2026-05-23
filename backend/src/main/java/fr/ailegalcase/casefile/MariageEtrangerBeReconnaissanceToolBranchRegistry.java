package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-02-04-06 — mariage-etranger-be-reconnaissance. */
@Component
public class MariageEtrangerBeReconnaissanceToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "mariage-etranger-be-reconnaissance";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
