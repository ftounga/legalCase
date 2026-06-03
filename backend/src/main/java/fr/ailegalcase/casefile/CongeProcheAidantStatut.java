package fr.ailegalcase.casefile;

/**
 * SF-218-47 : verdict d'éligibilité au congé de proche aidant (art. L.3142-16 à
 * L.3142-27 CT, F-DT-79). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>ELIGIBLE : la personne aidée réside en France/EEE de façon stable et
 *       régulière (art. L.3142-16).</li>
 *   <li>NON_ELIGIBLE : la personne aidée ne réside pas en France/EEE.</li>
 * </ul>
 */
public enum CongeProcheAidantStatut {
    ELIGIBLE,
    NON_ELIGIBLE
}
