package fr.ailegalcase.casefile;

/**
 * SF-219-17 : motif de départ du travailleur — détermine si la clause
 * d'écolage peut être actionnée.
 *
 * <p>L'art. 22bis § 3 de la Loi du 03/07/1978 sur les contrats de
 * travail prévoit que la clause d'écolage ne peut <b>pas</b> être
 * actionnée en cas de rupture du contrat <i>du fait de l'employeur</i>
 * (licenciement sans motif grave imputable au travailleur), en cas de
 * rupture pour motif grave dans le chef de l'employeur, ni à
 * l'expiration normale d'un CDD. Seule la démission volontaire du
 * travailleur (ou la rupture pour motif grave imputable à celui-ci)
 * peut déclencher le remboursement.</p>
 *
 * <h2>Régimes</h2>
 * <ul>
 *   <li>{@link #DEMISSION_TRAVAILLEUR} — démission volontaire du
 *       travailleur — la clause peut être actionnée (sous réserve des
 *       autres conditions de validité).</li>
 *   <li>{@link #LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE} — licenciement
 *       sans motif grave par l'employeur — clause inopposable.</li>
 *   <li>{@link #LICENCIEMENT_MOTIF_GRAVE_TRAVAILLEUR} — licenciement
 *       pour motif grave imputable au travailleur — la clause peut être
 *       actionnée.</li>
 *   <li>{@link #RUPTURE_MOTIF_GRAVE_EMPLOYEUR} — rupture du contrat
 *       pour motif grave dans le chef de l'employeur (démission act
 *       equipollent à rupture) — clause inopposable.</li>
 *   <li>{@link #FIN_CDD_NORMALE} — expiration normale d'un CDD —
 *       clause inopposable.</li>
 * </ul>
 */
public enum ClauseEcolageBeMotifDepart {
    /** Démission volontaire du travailleur — clause actionnable. */
    DEMISSION_TRAVAILLEUR,

    /** Licenciement sans motif grave par l'employeur — clause inopposable. */
    LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE,

    /** Licenciement pour motif grave imputable au travailleur — clause actionnable. */
    LICENCIEMENT_MOTIF_GRAVE_TRAVAILLEUR,

    /** Rupture pour motif grave dans le chef de l'employeur — clause inopposable. */
    RUPTURE_MOTIF_GRAVE_EMPLOYEUR,

    /** Expiration normale d'un CDD — clause inopposable. */
    FIN_CDD_NORMALE
}
