package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-217-02 : requête HTTP pour l'endpoint de suivi de la procédure de
 * liquidation-partage belge (notaire commis).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record LiquidationPartageBeRequest(
        Boolean notaireDesigne,
        LocalDate dateDesignationNotaire,
        Boolean operationsOuvertes,
        LocalDate dateOuvertureOperations,
        Boolean inventaireEtabli,
        Boolean projetLiquidationEtabli,
        LocalDate dateNotificationProjet,
        Boolean contreditsDeposes,
        Boolean procesVerbalDiresEtabli,
        Boolean homologationDemandee,
        LocalDate dateHomologation,
        String commentaire
) {

    LiquidationPartageBeInput toInput() {
        return new LiquidationPartageBeInput(
                notaireDesigne,
                dateDesignationNotaire,
                operationsOuvertes,
                dateOuvertureOperations,
                inventaireEtabli,
                projetLiquidationEtabli,
                dateNotificationProjet,
                contreditsDeposes,
                procesVerbalDiresEtabli,
                homologationDemandee,
                dateHomologation,
                commentaire
        );
    }
}
