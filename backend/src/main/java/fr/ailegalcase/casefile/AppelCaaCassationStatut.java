package fr.ailegalcase.casefile;

/**
 * SF-214-33 : statut du délai d'appel devant la Cour administrative d'appel (CAA)
 * après un jugement de tribunal administratif (TA) en contentieux des étrangers.
 *
 * <ul>
 *   <li>APPEL_POSSIBLE : le délai d'appel court, joursRestants &gt; 15 — marge confortable.</li>
 *   <li>URGENT : joursRestants ∈ [1, 15] — fenêtre courte, action prioritaire.</li>
 *   <li>PRESCRIT : joursRestants ≤ 0 — délai d'appel expiré.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (contentieux administratif des étrangers).
 */
public enum AppelCaaCassationStatut {
    APPEL_POSSIBLE,
    URGENT,
    PRESCRIT
}
