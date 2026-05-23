package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** F-JU-03 / SF-JU-03-03 — F-IM-09-aes-humanitaire. */
@Component
public class AesHumanitaireToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-IM-09-aes-humanitaire";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
