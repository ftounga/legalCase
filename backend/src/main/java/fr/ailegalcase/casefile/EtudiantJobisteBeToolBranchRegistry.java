package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-13 — branches valides de l'outil
 * {@code etudiant-jobiste-be} (BELGIQUE) pour le mapping jurisprudence.
 * V1 = single branch {@code default} : l'outil produit un verdict
 * d'éligibilité unique au régime du contrat d'occupation d'étudiants,
 * la base juridique commune (Loi 03/07/1978 + Loi-programme 24/12/2002
 * + AR 14/07/1995 + Loi-programme 22/12/2023) ne justifie pas une
 * segmentation jurisprudence par verdict ou par catégorie d'étudiant.
 */
@Component
public class EtudiantJobisteBeToolBranchRegistry implements ToolBranchRegistry {
    static final String TOOL_ID = "etudiant-jobiste-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
