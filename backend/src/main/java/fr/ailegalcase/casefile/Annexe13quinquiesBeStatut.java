package fr.ailegalcase.casefile;

/**
 * SF-215-17 : statut du délai de recours en annulation devant le Conseil du
 * Contentieux des Étrangers (CCE) contre l'Annexe 13quinquies — 30 jours
 * calendaires depuis la notification (art. 39/82 §4 al. 1 Loi 15/12/1980).
 *
 * <ul>
 *   <li>DISPONIBLE : joursRestants &gt; 10 — le délai court, marge confortable.</li>
 *   <li>URGENT : joursRestants ∈ [1, 10] — fenêtre courte, action prioritaire.</li>
 *   <li>EXPIRE : joursRestants ≤ 0 — délai de 30 jours dépassé.</li>
 *   <li>FORME : un recours a déjà été introduit (prioritaire sur les autres états).</li>
 * </ul>
 */
public enum Annexe13quinquiesBeStatut {
    DISPONIBLE,
    URGENT,
    EXPIRE,
    FORME
}
