package fr.ailegalcase.casefile;

/**
 * SF-221-06 : verdict de l'analyse du titre de séjour « victime de la traite des êtres
 * humains » en Belgique (art. 61/2 et s. Loi du 15/12/1980 ; circulaire du 26/09/2008 ;
 * accompagnement par un centre spécialisé agréé PAG-ASA / Sürya / Payoke).
 *
 * <p>Régime BE PROPRE (3 phases : délai de réflexion → titre temporaire → titre lié à la
 * procédure pénale), <b>DISTINCT</b> du pendant FR {@code F-IM-35-victime-traite-l4251-fr}
 * (régime L. 425-1 CESEDA, base juridique propre).
 *
 * <ul>
 *   <li>DELAI_REFLEXION : période de réflexion (~45 j) avant déclaration — accompagnement
 *       obligatoire par un centre agréé.</li>
 *   <li>ELIGIBLE_TITRE_TEMPORAIRE : rupture avec le réseau + accompagnement par un centre
 *       spécialisé + déclaration faite / procédure pénale en cours — titre temporaire ouvert.</li>
 *   <li>ELIGIBLE_SOUS_PROCEDURE_PENALE : coopération judiciaire + procédure pénale en cours —
 *       titre lié à l'utilité de la déclaration.</li>
 *   <li>CONDITIONS_NON_REUNIES : pas de rupture avec le réseau OU pas d'accompagnement par un
 *       centre spécialisé.</li>
 *   <li>A_ORIENTER_CENTRE : aucune phase engagée — orienter vers un centre spécialisé
 *       (PAG-ASA / Sürya / Payoke) avant toute démarche.</li>
 * </ul>
 */
public enum VictimeTraiteBeVerdict {
    DELAI_REFLEXION,
    ELIGIBLE_TITRE_TEMPORAIRE,
    ELIGIBLE_SOUS_PROCEDURE_PENALE,
    CONDITIONS_NON_REUNIES,
    A_ORIENTER_CENTRE
}
