package fr.ailegalcase.casefile;

/**
 * SF-214-29 : statut du délai de recours devant le Tribunal judiciaire contre
 * un refus de déclaration de nationalité française — 6 mois à compter du refus
 * (Cciv 26-3).
 *
 * <ul>
 *   <li>RECOURS_POSSIBLE : le délai court, joursRestants &gt; 30 — marge confortable.</li>
 *   <li>URGENT : joursRestants ∈ [1, 30] — fenêtre courte, action prioritaire.</li>
 *   <li>PRESCRIT : joursRestants ≤ 0 — délai de 6 mois dépassé, recours irrecevable.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit de la nationalité française).
 */
public enum NaturalisationRecoursTjStatut {
    RECOURS_POSSIBLE,
    URGENT,
    PRESCRIT
}
