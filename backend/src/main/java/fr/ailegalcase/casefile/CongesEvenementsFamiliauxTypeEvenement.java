package fr.ailegalcase.casefile;

/**
 * SF-218-43 : nature de l'évènement familial ouvrant droit à un congé pour
 * évènement familial (art. L.3142-1 à L.3142-5 CT, F-DT-76). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>MARIAGE_PACS : mariage ou conclusion d'un PACS du salarié → 4 jours.</li>
 *   <li>NAISSANCE : naissance ou arrivée d'un enfant adopté → 3 jours.</li>
 *   <li>DECES_ENFANT : décès d'un enfant → 5 jours (porté à 7 jours ouvrés dans
 *       les cas renforcés).</li>
 *   <li>DECES_CONJOINT_PARTENAIRE : décès du conjoint, du partenaire de PACS ou
 *       du concubin → 3 jours.</li>
 *   <li>DECES_PERE_MERE : décès du père, de la mère, du beau-père, de la
 *       belle-mère, d'un frère ou d'une sœur → 3 jours.</li>
 *   <li>ANNONCE_HANDICAP_ENFANT : annonce de la survenue d'un handicap, d'un
 *       cancer ou d'une pathologie chronique chez un enfant → 2 jours.</li>
 *   <li>DEMENAGEMENT_NON_LEGAL : déménagement — aucun congé légal pour évènement
 *       familial ; renvoi à une éventuelle disposition conventionnelle → 0 jour
 *       légal.</li>
 * </ul>
 */
public enum CongesEvenementsFamiliauxTypeEvenement {
    MARIAGE_PACS,
    NAISSANCE,
    DECES_ENFANT,
    DECES_CONJOINT_PARTENAIRE,
    DECES_PERE_MERE,
    ANNONCE_HANDICAP_ENFANT,
    DEMENAGEMENT_NON_LEGAL
}
