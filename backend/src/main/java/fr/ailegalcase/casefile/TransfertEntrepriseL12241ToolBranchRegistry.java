package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-DT-72 transfert d'entreprise L. 1224-1 FR. */
@Component
public class TransfertEntrepriseL12241ToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-72-transfert-entreprise-l1224-1";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
