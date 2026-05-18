package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-217-04 : résultat structuré de l'arbre décisionnel de l'autorité parentale
 * belge (CC art. 374-375).
 */
public record AutoriteParentaleBeResult(
        AutoriteParentaleBeCalculator.Verdict verdict,
        AutoriteParentaleBeCalculator.VoieProcedurale voieProcedurale,
        List<AutoriteParentaleBeCalculator.Facteur> facteurs,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {
}
