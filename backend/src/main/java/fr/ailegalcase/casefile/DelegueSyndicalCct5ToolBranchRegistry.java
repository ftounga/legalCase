package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-10 — branches valides de l'outil
 * {@code delegue-syndical-cct-5} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict de statut unique (entreprise dans le champ + désignation
 * régulière + notification) ; les sous-régimes (statut reconnu /
 * fragile / inéligible) ne justifient pas une segmentation
 * jurisprudence séparée car la base juridique reste la même — CCT
 * n° 5 + CCT sectorielles.
 */
@Component
public class DelegueSyndicalCct5ToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "delegue-syndical-cct-5";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
