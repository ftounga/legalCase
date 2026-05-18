package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-217-08 : input du calcul d'analyse de la pension alimentaire entre ex-époux
 * belge (CC art. 301).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.</p>
 */
public record ContributionConjointBeInput(
        ContributionConjointBeCalculator.TypeDivorce typeDivorce,
        Boolean renonciationPensionConvention,
        Boolean creancierEnEtatDeBesoin,
        Boolean fauteGraveCreancier,
        int dureeMariageAnnees,
        BigDecimal revenuMensuelCreancier,
        BigDecimal revenuMensuelDebiteur,
        Boolean degradationEconomiqueLieeAuMariage,
        String commentaire
) {
}
