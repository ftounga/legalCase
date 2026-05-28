package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-08 — branches valides de l'outil
 * {@code transfert-entreprise-cct-32bis} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit un
 * verdict de conformité unique (qualification + droits maintenus) ; les
 * sous-régimes (transfert conforme / non conforme info-consult /
 * inéligibilité) ne justifient pas une segmentation jurisprudence
 * séparée car la base juridique reste la même — CCT n° 32bis +
 * Loi 17/03/1965 + Directive 2001/23/CE.
 */
@Component
public class TransfertEntrepriseCct32bisToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "transfert-entreprise-cct-32bis";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
