package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-JU-03-99c — déclare les branches de calcul valides de l'outil
 * F-IM-23-crrv-refus-visa-fr pour le cron dérive (SF-JU-01-03).
 *
 * <p>V1 : une seule branche {@code default}.</p>
 */
@Component
public class CrrvRefusVisaToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-IM-23-crrv-refus-visa-fr";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
