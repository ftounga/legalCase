package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-JU-03-01 — déclare les branches de calcul valides de l'outil
 * F-DT-07 ancienneté + congés + prime pour le cron dérive (SF-JU-01-03).
 *
 * <p>V1 : une seule branche {@code default}. Si le calculator se ramifie
 * (ex. différences avec/sans clause CCN), enrichir cette liste pour permettre
 * un mapping plus fin et permettre au cron dérive de détecter une suppression
 * de branche côté code.</p>
 */
@Component
public class AncienneteToolBranchRegistry implements ToolBranchRegistry {

    static final String TOOL_ID = "F-DT-07-anciennete-conges-prime";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
