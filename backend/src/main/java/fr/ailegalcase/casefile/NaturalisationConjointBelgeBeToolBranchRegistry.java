package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 — F-IM-29-naturalisation-conjoint-belge-be (F-215 P2 Immigration BE / SF-215-09). */
@Component
public class NaturalisationConjointBelgeBeToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-IM-29-naturalisation-conjoint-belge-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
