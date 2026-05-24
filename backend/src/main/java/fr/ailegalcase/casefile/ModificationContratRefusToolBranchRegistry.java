package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-DT-70 modification du contrat — refus du salarié (FR). */
@Component
public class ModificationContratRefusToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-70-modification-contrat-refus";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
