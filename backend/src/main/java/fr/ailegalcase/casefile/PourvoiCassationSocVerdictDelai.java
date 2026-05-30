package fr.ailegalcase.casefile;

/**
 * SF-218-05 : verdict du délai du pourvoi en cassation (2 mois à compter de la
 * notification de l'arrêt — art. 612 CPC). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>DELAI_OUVERT : le délai court, plus de 14 jours restants ;</li>
 *   <li>DELAI_URGENT : 14 jours ou moins avant l'expiration (déclaration de
 *       pourvoi à former sans délai) ;</li>
 *   <li>DELAI_EXPIRE : le délai de 2 mois est dépassé, le pourvoi est
 *       irrecevable (forclusion).</li>
 * </ul>
 */
public enum PourvoiCassationSocVerdictDelai {
    DELAI_OUVERT,
    DELAI_URGENT,
    DELAI_EXPIRE
}
