package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-DT-77 congé maternité / paternité (FR). */
@Component
public class CongeMaternitePaterniteToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-77-conge-paternite-maternite";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
