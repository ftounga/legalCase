package fr.ailegalcase.casefile;

/**
 * SF-216-21 : qualité du receleur envisagé (art. 778 Cciv).
 *
 * <ul>
 *   <li>{@link #HERITIER} — héritier légal ou réservataire visé par la
 *       sanction de l'art. 778 al. 2 (privation + rapport sans
 *       émolument).</li>
 *   <li>{@link #LEGATAIRE} — légataire universel ou à titre universel
 *       (étendue de l'art. 778 par jurisprudence).</li>
 *   <li>{@link #DONATAIRE} — donataire rapportable, visé par le recel
 *       de donation (art. 778 + 850 Cciv).</li>
 *   <li>{@link #TIERS_COMPLICITE} — tiers complice : pas concerné par
 *       la sanction successorale art. 778 ; voie pénale ou action
 *       délictuelle.</li>
 * </ul>
 */
public enum ReceleurQualiteEnum {
    HERITIER,
    LEGATAIRE,
    DONATAIRE,
    TIERS_COMPLICITE
}
