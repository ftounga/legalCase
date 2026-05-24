package fr.ailegalcase.casefile;

/**
 * SF-212-09 : requête HTTP pour l'endpoint d'évaluation de la faute
 * inexcusable de l'employeur (F-DT-91, FRANCE — L. 452-1 à L. 452-5 CSS).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record FauteInexcusableEmployeurRequest(
        Boolean conscienceDangerEmployeurEtablie,
        Boolean signalementDangerPrior,
        Boolean mesuresPreventionPrises,
        Boolean documentUniqueEvalue,
        Boolean formationSecuriteProdiguee,
        double tauxIpp,
        Double renteMensuelleEuros,
        double salaireMensuelBrutEuros
) {

    FauteInexcusableEmployeurInput toInput() {
        return new FauteInexcusableEmployeurInput(
                conscienceDangerEmployeurEtablie,
                signalementDangerPrior,
                mesuresPreventionPrises,
                documentUniqueEvalue,
                formationSecuriteProdiguee,
                tauxIpp,
                renteMensuelleEuros,
                salaireMensuelBrutEuros
        );
    }
}
