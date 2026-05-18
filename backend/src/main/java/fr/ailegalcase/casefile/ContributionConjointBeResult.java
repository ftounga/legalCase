package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-217-08 : résultat structuré de l'analyse de la pension alimentaire entre
 * ex-époux belge (CC art. 301).
 */
public record ContributionConjointBeResult(
        ContributionConjointBeCalculator.Verdict verdict,
        int dureeMaximaleMois,
        BigDecimal montantMensuelIndicatif,
        BigDecimal plafondTiersRevenusDebiteur,
        List<ContributionConjointBeCalculator.MotifExclusion> motifsExclusion,
        List<String> detailCalcul,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {
}
