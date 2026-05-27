package fr.ailegalcase.casefile;

/**
 * SF-219-04 : verdict de l'outil cumul RCC BE + allocations.
 *
 * <p>L'outil vérifie si le cumul {@code allocation de chômage ONEM +
 * indemnité complémentaire CCT n° 17} respecte les règles de plafond
 * (pas supérieur à la rémunération nette de référence) et les
 * conditions de disponibilité ajustée selon l'âge.</p>
 *
 * <ul>
 *   <li>{@link #CUMUL_CONFORME} — montant total ≤ référence nette,
 *       conditions de disponibilité ajustée respectées (ou bascule
 *       pension applicable).</li>
 *   <li>{@link #CUMUL_PLAFOND_DEPASSE} — total > rémunération nette de
 *       référence → indemnité complémentaire à réduire (plancher
 *       interdit, l'employeur ne peut pas faire mieux que la dernière
 *       rémunération nette).</li>
 *   <li>{@link #BASCULE_PENSION_LEGALE} — l'âge légal de la pension est
 *       atteint à la date considérée : le RCC s'éteint et l'intéressé
 *       bascule sur la pension légale (le cumul n'a plus d'objet).</li>
 *   <li>{@link #INCOMPATIBLE_ACTIVITE_NON_AUTORISEE} — reprise d'activité
 *       salariée à temps plein non déclarée / non autorisée → sortie du
 *       régime (perte des allocations + indemnité complémentaire,
 *       régularisation ONEM).</li>
 * </ul>
 */
public enum CumulRccAllocationsVerdict {
    /** Cumul conforme — total ≤ rémunération nette de référence + disponibilité OK. */
    CUMUL_CONFORME,

    /** Total > rémunération nette de référence — indemnité complémentaire à réduire. */
    CUMUL_PLAFOND_DEPASSE,

    /** Âge légal pension atteint — RCC s'éteint, bascule pension légale. */
    BASCULE_PENSION_LEGALE,

    /** Activité reprise non autorisée — sortie du régime, perte des droits cumulés. */
    INCOMPATIBLE_ACTIVITE_NON_AUTORISEE
}
