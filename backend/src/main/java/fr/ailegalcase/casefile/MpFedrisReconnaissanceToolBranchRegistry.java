package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-28 — branches valides de l'outil
 * {@code mp-fedris-reconnaissance} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : l'outil produit
 * un verdict unique (presomption liste fermee / exposition insuffisante
 * / systeme ouvert causalite etablie ou insuffisante / prescription /
 * a qualifier), la base juridique commune (Lois coordonnees du
 * 03/06/1970 + AR du 28/03/1969 + AR du 16/12/1985) ne justifie pas
 * une segmentation jurisprudence par verdict.
 */
@Component
public class MpFedrisReconnaissanceToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "mp-fedris-reconnaissance";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
