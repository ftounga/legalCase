package fr.ailegalcase.casefile;

/**
 * SF-212-09 : données d'entrée du calculateur d'évaluation de la faute
 * inexcusable de l'employeur (F-DT-91, FRANCE — L. 452-1 à L. 452-5 CSS ;
 * Cass. ass. plén. 24/06/2005 ; L. 4121-1 CT).
 *
 * @param conscienceDangerEmployeurEtablie  l'employeur avait/aurait dû avoir
 *                                          conscience du danger (1re condition
 *                                          cumulative Cass. ass. plén. 24/06/2005)
 * @param signalementDangerPrior            le salarié, un collègue, le CSE,
 *                                          le médecin du travail ou l'inspection
 *                                          du travail a signalé le danger AVANT
 *                                          l'AT/MP
 * @param mesuresPreventionPrises           l'employeur a pris les mesures
 *                                          nécessaires de prévention (2nde
 *                                          condition cumulative ; obligation
 *                                          L. 4121-1 CT)
 * @param documentUniqueEvalue              DUERP existe et à jour (R. 4121-1 CT)
 * @param formationSecuriteProdiguee        formation sécurité organisée
 *                                          (L. 4141-2 CT)
 * @param tauxIpp                           taux d'IPP reconnu par CPAM en %
 *                                          (0 à 100)
 * @param renteMensuelleEuros               montant mensuel de la rente AT/MP
 *                                          existante (null si pas de rente)
 * @param salaireMensuelBrutEuros           salaire mensuel brut de référence
 *                                          (≥ 0)
 */
public record FauteInexcusableEmployeurInput(
        Boolean conscienceDangerEmployeurEtablie,
        Boolean signalementDangerPrior,
        Boolean mesuresPreventionPrises,
        Boolean documentUniqueEvalue,
        Boolean formationSecuriteProdiguee,
        double tauxIpp,
        Double renteMensuelleEuros,
        double salaireMensuelBrutEuros
) {}
