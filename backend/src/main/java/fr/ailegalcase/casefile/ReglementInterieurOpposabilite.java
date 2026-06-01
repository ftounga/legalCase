package fr.ailegalcase.casefile;

/**
 * SF-218-35 : opposabilité d'un règlement intérieur aux salariés (F-DT-100).
 * Outil <b>FRANCE UNIQUEMENT</b>. Conditionnée au respect de la procédure de mise
 * en place (consultation du CSE, transmission à l'inspection du travail, dépôt au
 * greffe du conseil de prud'hommes — art. L.1321-4 CT).
 *
 * <ul>
 *   <li>OPPOSABLE : la procédure de mise en place est respectée → le règlement
 *       intérieur est opposable aux salariés.</li>
 *   <li>INOPPOSABLE : une formalité de la procédure manque → le règlement
 *       intérieur est inopposable aux salariés (art. L.1321-4).</li>
 * </ul>
 */
public enum ReglementInterieurOpposabilite {
    OPPOSABLE,
    INOPPOSABLE
}
