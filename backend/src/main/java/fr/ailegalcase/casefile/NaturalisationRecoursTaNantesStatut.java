package fr.ailegalcase.casefile;

/**
 * SF-214-31 : statut du délai de recours devant le Tribunal administratif de
 * Nantes contre un refus de naturalisation par décret — 2 mois à compter de la
 * notification du refus (CJA droit commun).
 *
 * <ul>
 *   <li>RECOURS_POSSIBLE : le délai court, joursRestants &gt; 15 — marge confortable.</li>
 *   <li>URGENT : joursRestants ∈ [1, 15] — fenêtre courte, action prioritaire.</li>
 *   <li>PRESCRIT : joursRestants ≤ 0 — délai de 2 mois dépassé, recours irrecevable.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (refus de naturalisation par décret —
 * juridiction administrative, compétence exclusive TA de Nantes ; distinct du
 * recours TJ refus de déclaration de nationalité, SF-214-29).
 */
public enum NaturalisationRecoursTaNantesStatut {
    RECOURS_POSSIBLE,
    URGENT,
    PRESCRIT
}
