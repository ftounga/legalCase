package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-15 — branches valides de l'outil
 * {@code interim-be-indemnite-fin-mission} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un calcul unifié des indemnités exigibles, la base juridique commune
 * (Loi 24/07/1987 + CCT n° 322/322bis + jurisprudence Cass. BE) ne
 * justifie pas une segmentation jurisprudence par composante d'indemnité.
 */
@Component
public class InterimBeIndemniteFinMissionToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "interim-be-indemnite-fin-mission";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
