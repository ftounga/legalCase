package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-223-09 — etat-civil-be-modification (Famille BELGIQUE). */
@Component
public class EtatCivilBeModificationToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "etat-civil-be-modification";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
