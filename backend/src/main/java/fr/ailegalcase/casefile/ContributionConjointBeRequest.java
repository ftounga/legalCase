package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-217-08 : requête HTTP pour l'endpoint d'analyse de la pension alimentaire
 * entre ex-époux belge.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record ContributionConjointBeRequest(
        ContributionConjointBeCalculator.TypeDivorce typeDivorce,
        Boolean renonciationPensionConvention,
        Boolean creancierEnEtatDeBesoin,
        Boolean fauteGraveCreancier,
        Integer dureeMariageAnnees,
        BigDecimal revenuMensuelCreancier,
        BigDecimal revenuMensuelDebiteur,
        Boolean degradationEconomiqueLieeAuMariage,
        String commentaire
) {

    ContributionConjointBeInput toInput() {
        return new ContributionConjointBeInput(
                typeDivorce,
                renonciationPensionConvention,
                creancierEnEtatDeBesoin,
                fauteGraveCreancier,
                dureeMariageAnnees == null ? -1 : dureeMariageAnnees,
                revenuMensuelCreancier,
                revenuMensuelDebiteur,
                degradationEconomiqueLieeAuMariage,
                commentaire
        );
    }
}
