package fr.ailegalcase.casefile;

/**
 * SF-219-23 : sanction éventuellement subie par le travailleur après
 * sa demande d'aménagement raisonnable. La survenance d'une sanction
 * dans la chronologie post-demande déclenche la <b>présomption de
 * représailles</b> de l'art. 17 Loi 10/05/2007 — renverse la charge
 * de la preuve sur l'employeur qui doit démontrer un motif étranger
 * à la demande d'aménagement, faute de quoi indemnité forfaitaire 6
 * mois de rémunération brute cumulable avec l'indemnité de rupture
 * de droit commun (art. 28 § 2 1°).
 *
 * <ul>
 *   <li>{@link #LICENCIEMENT} — licenciement après la demande — la
 *       présomption joue, qualification REPRESAILLES_PRESUMEES_LICENCIEMENT.</li>
 *   <li>{@link #REGRESSION_CONDITIONS_TRAVAIL} — déclassement,
 *       modification substantielle unilatérale, mutation
 *       défavorable — la présomption joue également (art. 17
 *       § 2 vise toute mesure préjudiciable).</li>
 *   <li>{@link #HARCELEMENT_REPRESAILLE} — comportement abusif
 *       caractérisé suite à la demande — articulation avec la
 *       procédure Loi 04/08/1996 art. 32bis-32sexies bien-être.</li>
 *   <li>{@link #AUCUNE} — aucune sanction, analyse purement
 *       discrimination indirecte.</li>
 * </ul>
 */
public enum DiscriminationBeHandicapAmenagementSanctionSubie {
    /** Licenciement post-demande — présomption représailles. */
    LICENCIEMENT,

    /** Régression conditions de travail — présomption représailles. */
    REGRESSION_CONDITIONS_TRAVAIL,

    /** Harcèlement représaille — articulation Loi 04/08/1996. */
    HARCELEMENT_REPRESAILLE,

    /** Aucune sanction subie. */
    AUCUNE
}
