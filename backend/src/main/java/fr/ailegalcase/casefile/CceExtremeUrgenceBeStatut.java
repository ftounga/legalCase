package fr.ailegalcase.casefile;

/**
 * SF-215-15 : statut du délai de recours en <b>extrême urgence</b> devant le
 * Conseil du Contentieux des Étrangers (CCE) — 5 jours <b>ouvrables</b> depuis
 * l'acte exécutoire imminent (art. 39/82 §4 al. 2-3 Loi 15/12/1980).
 *
 * <p>À la différence de F-IM-31 (recours en annulation, 30 jours calendaires),
 * le recours en extrême urgence se compte en jours ouvrables belges
 * ({@link fr.ailegalcase.shared.BelgianBusinessDaysCalculator}) et vise un acte
 * d'éloignement IMMINENT (rapatriement, transfert Dublin, expulsion).
 *
 * <ul>
 *   <li>DISPONIBLE : joursOuvrablesRestants &gt; 2 — marge d'action subsiste.</li>
 *   <li>CRITIQUE : joursOuvrablesRestants ∈ [1, 2] — fenêtre quasi fermée,
 *       requête en suspension à introduire en urgence absolue.</li>
 *   <li>EXPIRE : joursOuvrablesRestants ≤ 0 — délai de 5 jours ouvrables dépassé.</li>
 *   <li>RECOURS_FORME : un recours en extrême urgence a déjà été introduit
 *       (prioritaire sur les autres états).</li>
 * </ul>
 */
public enum CceExtremeUrgenceBeStatut {
    DISPONIBLE,
    CRITIQUE,
    EXPIRE,
    RECOURS_FORME
}
