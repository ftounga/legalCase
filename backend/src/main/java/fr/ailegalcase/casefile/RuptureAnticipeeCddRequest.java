package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-212-17 : requête HTTP pour l'endpoint d'analyse de la rupture anticipée
 * d'un CDD (F-DT-43-rupture-anticipee-cdd, FRANCE — L. 1243-1 à L. 1243-4 CT ;
 * L. 1243-8 CT — indemnité de précarité ; Cass. soc. constante).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record RuptureAnticipeeCddRequest(
        RuptureAnticipeeCddInput.AuteurRupture auteurRupture,
        RuptureAnticipeeCddInput.MotifRupture motifRupture,
        LocalDate dateTermeCdd,
        LocalDate dateRupture,
        double salaireMensuelBrutEuros,
        double remunerationBruteTotaleContratEuros
) {

    RuptureAnticipeeCddInput toInput() {
        return new RuptureAnticipeeCddInput(
                auteurRupture,
                motifRupture,
                dateTermeCdd,
                dateRupture,
                salaireMensuelBrutEuros,
                remunerationBruteTotaleContratEuros
        );
    }
}
