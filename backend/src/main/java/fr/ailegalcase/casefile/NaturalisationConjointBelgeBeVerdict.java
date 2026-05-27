package fr.ailegalcase.casefile;

/**
 * SF-215-09 : verdict d'éligibilité à la déclaration de nationalité belge par mariage
 * avec un(e) Belge — art. 16 Code de la nationalité belge (loi 28/06/1984 consolidée,
 * loi modificative 04/12/2012).
 *
 * <p>Contrairement à la naturalisation 12bis (voies 5/10 ans — {@link
 * Naturalisation12bisBeVoieEligible}), l'art. 16 ne propose qu'une seule voie : verdict
 * binaire ELIGIBLE / INELIGIBLE.
 *
 * <ul>
 *   <li>ELIGIBLE : toutes les conditions cumulatives sont satisfaites — durée cohabitation
 *       ≥ 5 ans + niveau langue ≥ A2 + preuve d'intégration + pas de menace à l'ordre
 *       public + pas de condamnation pénale ≥ 3 mois ferme dans les 5 ans.</li>
 *   <li>INELIGIBLE : au moins une condition n'est pas remplie — la déclaration art. 16
 *       n'est pas recevable en l'état (consulter {@code criteresNonRemplis} pour le
 *       détail).</li>
 * </ul>
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — voir aussi :
 * <ul>
 *   <li>SF-215-07 (naturalisation 12bis BE — voie résidence 5/10 ans, sans condition de
 *       mariage avec un Belge) ;</li>
 *   <li>F-IM-13 (naturalisation FR — Cciv art. 21-2 par mariage / art. 21-15 par décret) ;</li>
 *   <li>F-221 (naturalisation par décret art. 19-21 CNB — très rare, P3).</li>
 * </ul>
 *
 * <p>Source : Code de la nationalité belge art. 16 §1 2° ; AR 14/01/2013 (preuve langue) ;
 * loi modificative 04/12/2012.
 */
public enum NaturalisationConjointBelgeBeVerdict {
    ELIGIBLE,
    INELIGIBLE
}
