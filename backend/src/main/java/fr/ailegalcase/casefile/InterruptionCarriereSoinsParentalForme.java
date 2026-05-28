package fr.ailegalcase.casefile;

/**
 * SF-219-32 : forme d'interruption de carrière pour congé parental BE —
 * AR du 29/10/1997 art. 3 (modifié par AR 12/12/2001 introduisant le
 * cinquième temps et AR 05/05/2019 introduisant le dixième temps).
 *
 * <p>Le droit individuel au congé parental de 4 mois équivalent temps
 * plein par enfant et par parent peut être pris sous trois formes
 * principales (la durée légale est multipliée selon le ratio
 * d'occupation maintenu) :</p>
 *
 * <ol>
 *   <li><b>TEMPS_PLEIN</b> — suspension complète des prestations pendant
 *       4 mois.</li>
 *   <li><b>MI_TEMPS</b> — réduction des prestations à mi-temps pendant
 *       8 mois (équivalent 4 mois ETP).</li>
 *   <li><b>CINQUIEME_TEMPS</b> — réduction d'un cinquième des
 *       prestations pendant 20 mois (équivalent 4 mois ETP — introduit
 *       par AR 12/12/2001).</li>
 * </ol>
 *
 * <p>Note : la forme {@code DIXIEME_TEMPS} (40 mois — AR 05/05/2019)
 * n'est ouverte que sur autorisation employeur — hors scope V1.</p>
 */
public enum InterruptionCarriereSoinsParentalForme {
    /** Interruption complète — 4 mois temps plein (AR 29/10/1997 art. 3). */
    TEMPS_PLEIN,

    /** Réduction mi-temps — 8 mois (AR 29/10/1997 art. 3). */
    MI_TEMPS,

    /** Réduction 1/5e — 20 mois (AR 12/12/2001 art. 1). */
    CINQUIEME_TEMPS
}
