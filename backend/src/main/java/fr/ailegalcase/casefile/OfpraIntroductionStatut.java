package fr.ailegalcase.casefile;

/**
 * SF-214-17 : statut du délai d'introduction de la demande d'asile à l'OFPRA —
 * 90 jours à compter de l'arrivée en France (art. R. 521-1 et s. CESEDA).
 *
 * <ul>
 *   <li>A_DEPOSER : le délai court, joursRestants &gt; 21 — marge confortable.</li>
 *   <li>URGENT : joursRestants ∈ [1, 21] — fenêtre courte, action prioritaire.</li>
 *   <li>EXPIRE : joursRestants ≤ 0 — délai des 90 jours dépassé, demande tardive
 *       (risque de placement en procédure accélérée L. 531-27).</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit d'asile français).
 */
public enum OfpraIntroductionStatut {
    A_DEPOSER,
    URGENT,
    EXPIRE
}
