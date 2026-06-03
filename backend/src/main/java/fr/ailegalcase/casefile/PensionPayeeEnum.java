package fr.ailegalcase.casefile;

/**
 * SF-222-01 : état de paiement de la pension alimentaire par l'autre parent,
 * déterminant pour le droit à l'allocation de soutien familial (ASF, art. L. 523-1
 * CSS). Famille FR uniquement.
 */
public enum PensionPayeeEnum {
    /** Pension fixée mais non payée — la CAF verse l'ASF et se subroge (ARIPA). */
    NON_PAYEE,
    /** Pension fixée et payée partiellement / d'un montant insuffisant — ASF différentielle. */
    PARTIELLE,
    /** Pension payée intégralement — pas de droit à l'ASF de ce chef. */
    PAYEE
}
