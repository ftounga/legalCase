package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-217-06 : requête HTTP pour l'endpoint d'estimation de la contribution
 * alimentaire des enfants belge.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record ContributionAlimentaireEnfantsBeRequest(
        Integer nombreEnfants,
        ContributionAlimentaireEnfantsBeCalculator.TrancheAge trancheAgeEnfants,
        BigDecimal revenuMensuelParent1,
        BigDecimal revenuMensuelParent2,
        BigDecimal coutMensuelGlobalEnfants,
        Integer nuitsHebergementParent1,
        Integer nuitsHebergementParent2,
        BigDecimal allocationsFamilialesMensuelles,
        BigDecimal fraisExtraordinairesMensuels,
        Boolean parentDebiteurEstParent1,
        String commentaire
) {

    ContributionAlimentaireEnfantsBeInput toInput() {
        return new ContributionAlimentaireEnfantsBeInput(
                nombreEnfants == null ? 0 : nombreEnfants,
                trancheAgeEnfants,
                revenuMensuelParent1,
                revenuMensuelParent2,
                coutMensuelGlobalEnfants,
                nuitsHebergementParent1 == null ? 0 : nuitsHebergementParent1,
                nuitsHebergementParent2 == null ? 0 : nuitsHebergementParent2,
                allocationsFamilialesMensuelles,
                fraisExtraordinairesMensuels,
                parentDebiteurEstParent1,
                commentaire
        );
    }
}
