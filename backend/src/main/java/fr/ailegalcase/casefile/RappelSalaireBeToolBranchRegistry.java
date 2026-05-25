package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil rappel-salaire-be (BELGIQUE) pour
 * le mapping jurisprudence. V1 = single branch {@code default} (le calcul
 * d'intérêts moratoires 10 % et la prescription Loi 03/07/1978 art. 15
 * s'appliquent uniformément à tous les arriérés de salaire ; les régimes
 * particuliers — sécurité sociale, indus, etc. — sont hors scope V1).
 */
@Component
public class RappelSalaireBeToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "rappel-salaire-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
