package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolBranchRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * F-JU-03 / SF-219-27 — branches valides de l'outil
 * {@code inastri-statut-travailleur-independant} (BELGIQUE) pour le
 * mapping jurisprudence. V1 = single branch {@code default} : l'outil
 * produit un verdict unique (indépendant confirmé / salarié requalifié
 * / faux indépendant présomption sectorielle / présomption générale
 * salariat / à qualifier), la base juridique commune (Loi 27/06/1969
 * + AR n° 38 du 27/07/1967 + Loi-programme I 27/12/2006 art. 328 à
 * 333 + art. 337/2) ne justifie pas une segmentation jurisprudence
 * par verdict.
 */
@Component
public class InastriStatutTravailleurIndependantToolBranchRegistry
        implements ToolBranchRegistry {
    static final String TOOL_ID = "inastri-statut-travailleur-independant";
    static final String BRANCHE_DEFAULT = "default";

    @Override
    public Set<String> knownBranches() {
        return Set.of(TOOL_ID + ":" + BRANCHE_DEFAULT);
    }
}
