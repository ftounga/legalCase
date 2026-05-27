package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil licenciement-be-protection-deleguee
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (l'outil produit un verdict binaire et une indemnité
 * forfaitaire — pas de sous-régimes calculatoires indépendants justifiant
 * une segmentation jurisprudence).
 */
@Component
public class LicenciementBeProtectionDelegueeToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "licenciement-be-protection-deleguee";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
