package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** SF-223-03 — kafala-be-recueil-legal (Famille BELGIQUE). */
@Component
public class KafalaBeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "kafala-be-recueil-legal";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
