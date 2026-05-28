package fr.ailegalcase.casefile;

/**
 * SF-219-32 : verdict de l'outil <i>Interruption de carrière pour
 * congé parental BE</i> (Loi de redressement du 22/01/1985 art. 99 à
 * 107quater + AR du 29/10/1997 + CCT n° 64 du 29/04/1997).
 *
 * <p>L'outil qualifie l'éligibilité au congé parental ONEM et calcule
 * la durée, les allocations forfaitaires et la fenêtre de protection
 * contre le licenciement art. 101 Loi 22/01/1985.</p>
 *
 * <p>Distinct de F-DT-29 {@code credit-temps-be} qui couvre le
 * crédit-temps CCT 103 (régime universel sans motif spécifique) ;
 * SF-219-32 (présente) couvre le congé parental régi spécifiquement
 * par la Loi 22/01/1985 et la CCT 64 — droit individuel par enfant
 * et par parent, conditions d'âge enfant, formes spécifiques.</p>
 *
 * <ul>
 *   <li>{@link #ELIGIBLE_COMPLET} — toutes conditions remplies, la
 *       demande peut aboutir sans difficulté (ancienneté ≥ 12 mois,
 *       enfant éligible, formalisme respecté, solde disponible).</li>
 *   <li>{@link #ELIGIBLE_AVEC_RESERVES} — éligibilité confirmée mais
 *       points de vigilance (délai notification limite, solde tendu,
 *       formalisme limite).</li>
 *   <li>{@link #INELIGIBLE_ANCIENNETE} — moins de 12 mois auprès du
 *       même employeur dans la période de 15 mois précédant la demande
 *       (art. 5 AR 29/10/1997).</li>
 *   <li>{@link #INELIGIBLE_AGE_ENFANT} — enfant ≥ 12 ans (sans
 *       handicap) ou ≥ 21 ans (avec handicap ≥ 66%) à la date du
 *       début de l'interruption (art. 4 AR 29/10/1997).</li>
 *   <li>{@link #INELIGIBLE_SOLDE_INSUFFISANT} — demande dépasse le
 *       solde individuel restant 4 mois ETP par enfant et par parent
 *       (art. 2 AR 29/10/1997).</li>
 *   <li>{@link #INELIGIBLE_FORMALISME} — notification non conforme :
 *       hors délai 2 à 3 mois avant le début, mode autre que lettre
 *       recommandée ou remise en main propre contre accusé
 *       (art. 6 AR 29/10/1997).</li>
 *   <li>{@link #DIFFERE_EMPLOYEUR} — l'employeur a notifié dans le
 *       mois un différé motivé maximum 6 mois pour raisons
 *       organisationnelles (art. 7 AR 29/10/1997).</li>
 *   <li>{@link #A_QUALIFIER} — inputs contradictoires ou insuffisants
 *       nécessitant analyse complémentaire avocat.</li>
 * </ul>
 */
public enum InterruptionCarriereSoinsParentalVerdict {
    /** Éligibilité complète sans difficulté apparente. */
    ELIGIBLE_COMPLET,

    /** Éligibilité avec points de vigilance procéduraux. */
    ELIGIBLE_AVEC_RESERVES,

    /** Ancienneté insuffisante — moins de 12 mois auprès de l'employeur. */
    INELIGIBLE_ANCIENNETE,

    /** Enfant hors plafond d'âge — 12 ans (ou 21 ans si handicap). */
    INELIGIBLE_AGE_ENFANT,

    /** Solde individuel insuffisant — droit 4 mois ETP épuisé. */
    INELIGIBLE_SOLDE_INSUFFISANT,

    /** Formalisme demande non conforme — délai ou mode notification. */
    INELIGIBLE_FORMALISME,

    /** Employeur a différé l'interruption (art. 7 AR 29/10/1997). */
    DIFFERE_EMPLOYEUR,

    /** Inputs insuffisants — analyse complémentaire avocat requise. */
    A_QUALIFIER
}
