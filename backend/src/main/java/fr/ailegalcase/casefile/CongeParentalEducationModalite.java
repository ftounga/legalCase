package fr.ailegalcase.casefile;

/**
 * SF-218-45 : modalité d'exercice du congé parental d'éducation (art. L.1225-47
 * CT, F-DT-78). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>TEMPS_PLEIN : suspension totale du contrat de travail (congé total).</li>
 *   <li>TEMPS_PARTIEL : réduction de la durée du travail (le salarié continue de
 *       travailler à temps partiel pour élever l'enfant).</li>
 * </ul>
 */
public enum CongeParentalEducationModalite {
    TEMPS_PLEIN,
    TEMPS_PARTIEL
}
