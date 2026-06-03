package fr.ailegalcase.casefile;

/**
 * SF-218-45 : verdict d'éligibilité au congé parental d'éducation (art. L.1225-47
 * CT, F-DT-78). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>ELIGIBLE : un an d'ancienneté minimum atteint à la date de naissance /
 *       d'arrivée de l'enfant (ancienneteMois &ge; 12).</li>
 *   <li>NON_ELIGIBLE : ancienneté insuffisante (ancienneteMois &lt; 12).</li>
 * </ul>
 */
public enum CongeParentalEducationStatut {
    ELIGIBLE,
    NON_ELIGIBLE
}
