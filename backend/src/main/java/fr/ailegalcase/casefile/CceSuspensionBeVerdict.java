package fr.ailegalcase.casefile;

/**
 * SF-221-05 : verdict de l'analyse du recours CCE en SUSPENSION ORDINAIRE
 * (référé administratif : art. 39/82 Loi 15/12/1980 ; loi 15/09/2006).
 *
 * <p>3e recours CCE distinct de l'annulation 30j (F-IM-31) et de l'extrême urgence
 * 5j ouvrables (F-IM-32). La suspension ordinaire suppose l'urgence (NON extrême) +
 * un risque de préjudice grave difficilement réparable, greffée sur la requête en
 * annulation 30j.
 *
 * <ul>
 *   <li>CONDITIONS_REUNIES : urgence + préjudice grave difficilement réparable réunis,
 *       requête en annulation introduite ou encore introductible (≤ 30 j), éloignement
 *       NON imminent — la suspension ordinaire est ouverte.</li>
 *   <li>URGENCE_NON_DEMONTREE : l'urgence n'est pas invoquée / démontrée — la suspension
 *       ordinaire ne peut prospérer (à vérifier par avocat).</li>
 *   <li>PREJUDICE_NON_DEMONTRE : le risque de préjudice grave difficilement réparable
 *       n'est pas démontré — condition de la suspension non réunie.</li>
 *   <li>HORS_DELAI_ANNULATION : le délai de 30 j de l'annulation est dépassé et aucun
 *       recours en annulation n'a été introduit — la suspension, qui s'y greffe, est
 *       compromise (à vérifier par avocat).</li>
 *   <li>REORIENTER_EXTREME_URGENCE : une mesure d'éloignement est imminente — l'extrême
 *       urgence (F-IM-32, 5 j ouvrables) prime sur la suspension ordinaire.</li>
 * </ul>
 */
public enum CceSuspensionBeVerdict {
    CONDITIONS_REUNIES,
    URGENCE_NON_DEMONTREE,
    PREJUDICE_NON_DEMONTRE,
    HORS_DELAI_ANNULATION,
    REORIENTER_EXTREME_URGENCE
}
