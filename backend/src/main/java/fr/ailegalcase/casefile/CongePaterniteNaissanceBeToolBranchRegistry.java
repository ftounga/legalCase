package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-31 — branches valides de l'outil
 * {@code conge-paternite-naissance-be} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique sur l'éligibilité au congé de naissance (Loi
 * 03/07/1978 art. 30 § 2 + Loi 07/04/2023), la base juridique commune
 * ne justifie pas une segmentation jurisprudence par verdict.
 */
@Component
public class CongePaterniteNaissanceBeToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "conge-paternite-naissance-be";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
