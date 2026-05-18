package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-217-06 : input du calcul de la contribution alimentaire des enfants belge
 * (méthode Renard — CC art. 203 / 203bis).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.</p>
 */
public record ContributionAlimentaireEnfantsBeInput(
        int nombreEnfants,
        ContributionAlimentaireEnfantsBeCalculator.TrancheAge trancheAgeEnfants,
        BigDecimal revenuMensuelParent1,
        BigDecimal revenuMensuelParent2,
        BigDecimal coutMensuelGlobalEnfants,
        int nuitsHebergementParent1,
        int nuitsHebergementParent2,
        BigDecimal allocationsFamilialesMensuelles,
        BigDecimal fraisExtraordinairesMensuels,
        Boolean parentDebiteurEstParent1,
        String commentaire
) {
}
