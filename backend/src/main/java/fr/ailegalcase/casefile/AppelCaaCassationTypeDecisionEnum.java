package fr.ailegalcase.casefile;

/**
 * SF-214-33 : sens de la décision rendue par le tribunal administratif (TA) dans
 * un contentieux des étrangers — détermine indirectement l'intérêt à faire appel.
 *
 * <ul>
 *   <li>REJET : le TA a rejeté la requête de l'étranger — appel à l'initiative de l'étranger.</li>
 *   <li>ANNULATION : le TA a annulé la décision administrative — appel à l'initiative
 *       de l'administration (préfet) ; l'étranger défend en appel.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (contentieux administratif des étrangers).
 */
public enum AppelCaaCassationTypeDecisionEnum {
    REJET,
    ANNULATION
}
