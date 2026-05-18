package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-217-06 : résultat structuré de l'estimation de contribution alimentaire
 * des enfants belge (méthode Renard).
 */
public record ContributionAlimentaireEnfantsBeResult(
        ContributionAlimentaireEnfantsBeCalculator.Verdict verdict,
        BigDecimal coutMensuelRetenu,
        BigDecimal coutNetApresAllocations,
        BigDecimal quotePartParent1Pct,
        BigDecimal quotePartParent2Pct,
        BigDecimal partContributiveParent1,
        BigDecimal partContributiveParent2,
        BigDecimal partHebergementParent1,
        BigDecimal partHebergementParent2,
        BigDecimal contributionMensuelleNette,
        ContributionAlimentaireEnfantsBeCalculator.ParentDebiteur parentDebiteur,
        BigDecimal fraisExtraordinairesQuotePartParent1,
        BigDecimal fraisExtraordinairesQuotePartParent2,
        List<String> detailCalcul,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {
}
