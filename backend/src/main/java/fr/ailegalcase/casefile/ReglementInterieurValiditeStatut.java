package fr.ailegalcase.casefile;

/**
 * SF-218-35 : verdict global de validité d'un règlement intérieur (art. L.1311-1
 * à L.1322-4, L.1321-1 et s. CT, F-DT-100). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>NON_REQUIS : effectif &lt; 50 salariés et aucun règlement intérieur
 *       existant → l'établissement d'un RI est facultatif (art. L.1311-2).</li>
 *   <li>CONFORME : tout le contenu obligatoire est présent, aucune clause
 *       interdite n'est stipulée et la procédure de mise en place est
 *       respectée.</li>
 *   <li>NON_CONFORME : un contenu obligatoire manque ou une clause interdite est
 *       présente (art. L.1321-1, L.1321-2, L.1321-3, L.1331-2).</li>
 *   <li>INOPPOSABLE : la procédure de mise en place n'est pas respectée
 *       (consultation CSE, transmission inspection ou dépôt greffe manquant) →
 *       le règlement intérieur est inopposable aux salariés (art. L.1321-4).</li>
 * </ul>
 */
public enum ReglementInterieurValiditeStatut {
    CONFORME,
    NON_CONFORME,
    INOPPOSABLE,
    NON_REQUIS
}
