package fr.ailegalcase.casefile;

/**
 * SF-221-04 : base légale du maintien en centre fermé (Loi du 15/12/1980).
 * Whitelist de 5 valeurs (à vérifier par avocat BE 2026) :
 *
 * <ul>
 *   <li>ART_7 : maintien art. 7 al. 3 (étranger faisant l'objet d'un ordre de quitter
 *       le territoire et maintenu en vue de l'éloignement).</li>
 *   <li>ART_27 : maintien art. 27 (exécution forcée de la mesure d'éloignement).</li>
 *   <li>ART_29 : maintien art. 29 (refoulement à la frontière).</li>
 *   <li>ART_74_5 : maintien art. 74/5 (étranger à la frontière, demande d'accès au
 *       territoire / lieu déterminé).</li>
 *   <li>AUTRE : autre base légale de maintien (à préciser par l'avocat).</li>
 * </ul>
 */
public enum DetentionBaseLegale {
    ART_7,
    ART_27,
    ART_29,
    ART_74_5,
    AUTRE
}
