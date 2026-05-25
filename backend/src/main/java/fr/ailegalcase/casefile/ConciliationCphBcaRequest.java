package fr.ailegalcase.casefile;

/**
 * SF-212-37 : requête HTTP pour l'endpoint de préparation de la conciliation
 * BCO du Conseil de Prud'hommes (F-DT-84-conciliation-cph-bca, FRANCE —
 * R. 1454-7 à R. 1454-12 CT ; L. 1235-1 al. 3 CT — barème transactions BCA).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record ConciliationCphBcaRequest(
        Integer ancienneteMois,
        Double salaireMensuelBrutEuros,
        Double montantDemandesEuros,
        ConciliationCphBcaInput.ConciliationCphBcaOpportunite opportuniteConciliation
) {

    ConciliationCphBcaInput toInput() {
        return new ConciliationCphBcaInput(
                ancienneteMois,
                salaireMensuelBrutEuros,
                montantDemandesEuros,
                opportuniteConciliation
        );
    }
}
