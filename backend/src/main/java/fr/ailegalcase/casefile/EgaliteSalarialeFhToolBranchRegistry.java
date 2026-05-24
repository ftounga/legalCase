package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-DT-56 égalité salariale femmes/hommes (FR). */
@Component
public class EgaliteSalarialeFhToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-56-egalite-salariale-femmes-hommes";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
