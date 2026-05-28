package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 — branches valides de l'outil licenciement-be-collectif-renault
 * (BELGIQUE) pour le mapping jurisprudence. V1 = single branch
 * {@code default} (l'outil produit un verdict de conformité procédurale ;
 * les sous-verdicts non-conformité par phase ne justifient pas une
 * segmentation jurisprudence séparée car la base juridique reste la même
 * — Loi 13/02/1998 + CCT n° 24 + CCT n° 39).
 */
@Component
public class LicenciementBeCollectifRenaultToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "licenciement-be-collectif-renault";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
