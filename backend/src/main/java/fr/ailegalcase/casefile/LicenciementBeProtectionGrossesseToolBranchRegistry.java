package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil licenciement-be-protection-grossesse
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (la protection art. 40 Loi 16/03/1971 produit une
 * matrice de verdict unique ; pas de sous-régimes calculatoires
 * indépendants).
 */
@Component
public class LicenciementBeProtectionGrossesseToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "licenciement-be-protection-grossesse";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
