package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-11 — branches valides de l'outil
 * {@code conge-education-paye-region} (BELGIQUE) pour le mapping
 * jurisprudence. V1 = single branch {@code default} : bien que l'outil
 * branche en interne sur 3 régimes régionaux (WBR / FLA / BXL), la
 * base juridique partagée (Loi 22/01/1985 + décrets régionaux de
 * transposition) ne justifie pas une segmentation jurisprudence par
 * région à ce stade — l'arrêt-clé de la Cour de cassation BE en
 * matière de congé-éducation payé reste référencé sous le tool_id
 * unique. Une segmentation par région pourra être introduite en V2
 * si une jurisprudence régionalement spécifique émerge.
 */
@Component
public class CongeEducationPayeRegionToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "conge-education-paye-region";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
