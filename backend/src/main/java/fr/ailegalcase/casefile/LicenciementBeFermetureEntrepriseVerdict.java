package fr.ailegalcase.casefile;

/**
 * SF-219-06 (mini-spec SF-219-07) : verdict de l'outil licenciement BE
 * fermeture d'entreprise — calculateur indemnité de fermeture + checklist
 * FFE (Fonds Fermeture Entreprises).
 *
 * <p>L'outil détermine si la fermeture déclarée est éligible aux régimes
 * du FFE (Loi du 26/06/2002, AR du 23/03/2007) et calcule à la fois
 * l'<b>indemnité de fermeture</b> (forfaitaire par année d'ancienneté +
 * supplément variable selon l'âge) et les créances activables auprès du
 * Fonds si l'employeur est en défaut de paiement (salaires impayés,
 * pécule de vacances, indemnité de rupture).</p>
 *
 * <ul>
 *   <li>{@link #ELIGIBLE_FFE_COMPLET} — fermeture qualifiée (cessation
 *       d'activité définitive, seuil d'effectif respecté, ancienneté ≥
 *       1 an, contrat en cours à la date de fermeture). Indemnité de
 *       fermeture due, créances FFE activables si insolvabilité.</li>
 *   <li>{@link #ELIGIBLE_INDEMNITE_SEULE} — droit à l'indemnité de
 *       fermeture seule : employeur solvable, le FFE n'intervient pas en
 *       reprise de créances impayées (cas standard cessation déclarée
 *       sans faillite).</li>
 *   <li>{@link #ELIGIBLE_FFE_REPRISE_CREANCES} — fermeture qualifiée +
 *       employeur insolvable : FFE intervient en reprise des créances
 *       salariales impayées (plafond légal). Indemnité de fermeture
 *       également due, à charge du FFE.</li>
 *   <li>{@link #INELIGIBLE_ANCIENNETE_INSUFFISANTE} — ancienneté &lt;
 *       1 an à la date de fermeture (Loi 26/06/2002, condition
 *       d'éligibilité).</li>
 *   <li>{@link #INELIGIBLE_TYPE_FERMETURE} — la fermeture déclarée
 *       n'entre pas dans les cas qualifiants (cessation partielle,
 *       transfert d'entreprise CCT 32bis, fusion sans suppression de
 *       postes, fermeture temporaire).</li>
 *   <li>{@link #INELIGIBLE_SEUIL_EFFECTIF} — l'entreprise n'atteint pas
 *       le seuil de 20 travailleurs équivalents temps plein (régime
 *       général FFE — seuil applicable au commerce / industrie ;
 *       régimes dérogatoires non couverts V1).</li>
 * </ul>
 */
public enum LicenciementBeFermetureEntrepriseVerdict {
    /** Fermeture qualifiée + insolvabilité = FFE reprend créances + indemnité. */
    ELIGIBLE_FFE_REPRISE_CREANCES,

    /** Fermeture qualifiée + insolvabilité avérée + indemnité due par FFE. */
    ELIGIBLE_FFE_COMPLET,

    /** Fermeture qualifiée + employeur solvable = indemnité de fermeture seule. */
    ELIGIBLE_INDEMNITE_SEULE,

    /** Ancienneté &lt; 1 an à la date de fermeture — pas d'indemnité FFE. */
    INELIGIBLE_ANCIENNETE_INSUFFISANTE,

    /** Type de fermeture non qualifiant (cessation partielle, transfert, fusion). */
    INELIGIBLE_TYPE_FERMETURE,

    /** Seuil d'effectif &lt; 20 ETP non atteint — pas d'éligibilité FFE V1. */
    INELIGIBLE_SEUIL_EFFECTIF
}
