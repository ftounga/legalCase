package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-16 — branches valides de l'outil
 * {@code teletravail-be-cct-85-149} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique de conformité de l'accord de télétravail, la base
 * juridique commune (CCT n° 85 + CCT n° 149 + Loi 03/10/2022) ne
 * justifie pas une segmentation jurisprudence par verdict ou par type.
 */
@Component
public class TeletravailBeCct85149ToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "teletravail-be-cct-85-149";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
