package fr.ailegalcase.casefile;

/**
 * SF-222-01 : verdict 4 niveaux de l'outil ASF (allocation de soutien familial,
 * art. L. 523-1 CSS). Famille FR uniquement.
 */
public enum VerdictAsfEnum {
    /** ASF à taux plein (autre parent décédé/inconnu, ou pension non fixée/non recouvrable). */
    DROIT_ASF_PLEIN,
    /** ASF différentielle (pension fixée < montant ASF et payée partiellement). */
    DROIT_ASF_DIFFERENTIELLE,
    /** ASF récupérable : pension fixée mais non payée → CAF verse + se subroge (ARIPA). */
    DROIT_AVEC_RECOUVREMENT,
    /** Pas de droit (parent non isolé, hors cas décès / inconnu). */
    PAS_DE_DROIT
}
