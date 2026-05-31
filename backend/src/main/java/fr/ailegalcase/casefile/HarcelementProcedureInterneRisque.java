package fr.ailegalcase.casefile;

/**
 * SF-218-27 : niveau de risque de responsabilité de l'employeur au titre de la
 * procédure interne de traitement d'un signalement de harcèlement (F-DT-59).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>ELEVE : verdict CARENCE_GRAVE (manquement à l'obligation de sécurité,
 *       L.4121-1).</li>
 *   <li>MODERE : verdict NON_CONFORME (item obligatoire manquant, hors enquête).</li>
 *   <li>FAIBLE : verdict CONFORME.</li>
 * </ul>
 */
public enum HarcelementProcedureInterneRisque {
    ELEVE,
    MODERE,
    FAIBLE
}
