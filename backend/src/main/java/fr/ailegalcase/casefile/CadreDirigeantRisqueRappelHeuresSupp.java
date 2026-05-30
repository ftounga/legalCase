package fr.ailegalcase.casefile;

/**
 * SF-218-19 : niveau de risque de rappel d'heures supplémentaires en cas de
 * contestation de la qualification de cadre dirigeant. Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>FAIBLE : qualification de cadre dirigeant confirmée (3 critères +
 *       participation effective à la direction) — exclusion des règles de durée
 *       du travail solidement établie.</li>
 *   <li>MODERE : qualification fragile (3 critères mais participation effective
 *       à la direction non établie) — risque de requalification et de rappel
 *       d'heures supplémentaires.</li>
 *   <li>ELEVE : qualification écartée (moins de 3 critères) — le salarié est
 *       soumis aux règles de durée du travail, rappel d'heures supplémentaires
 *       probable.</li>
 * </ul>
 */
public enum CadreDirigeantRisqueRappelHeuresSupp {
    FAIBLE,
    MODERE,
    ELEVE
}
