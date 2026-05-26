package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil licenciement-be-statut-unique-preavis
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (le barème statut unique post-2014 est uniforme — pas de
 * sous-régimes calculatoires distincts ; la formule Claeys pré-2014 fera
 * l'objet d'un outil séparé en SF-213-04).
 */
@Component
public class LicenciementBeStatutUniquePreavisToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "licenciement-be-statut-unique-preavis";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
