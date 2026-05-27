package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil licenciement-be-formule-claeys
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (la formule jurisprudentielle Claeys est uniforme — pas
 * de sous-régimes calculatoires distincts ; la cumulation avec le statut
 * unique post-2014 se fait via la même formule + barème).
 */
@Component
public class LicenciementBeFormuleClaeysToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "licenciement-be-formule-claeys";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
