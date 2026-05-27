package fr.ailegalcase.casefile;

/**
 * SF-213-06 : verdict de l'analyseur de validité d'une transaction de fin
 * de contrat en droit belge (art. 2044 Code civil belge + Loi 03/07/1978
 * art. 6).
 *
 * <h2>4 états possibles</h2>
 * <ul>
 *   <li>{@link #VALIDE} — concessions réciproques décrites, mention de
 *       contestation présente, renonciations expresses listées, pas de
 *       renonciation à un droit d'ordre public détectée.</li>
 *   <li>{@link #INVALIDE} — absence de concessions réciproques de l'employeur
 *       (art. 2044 Cciv : il s'agit alors d'une quittance, pas d'une
 *       transaction).</li>
 *   <li>{@link #INVALIDE_PARTIELLE} — renonciation à un droit d'ordre public
 *       détectée (Loi 03/07/1978 art. 6 : dispositions impératives — clause
 *       partiellement nulle, le reste de la transaction peut subsister).</li>
 *   <li>{@link #A_COMPLETER} — élément formel manquant (litige non visé ou
 *       checklist renonciations vide) — l'avocat doit compléter le document
 *       avant signature pour sécuriser la validité.</li>
 * </ul>
 */
public enum TransactionBeTravailVerdict {
    /** Transaction valide selon les éléments saisis. */
    VALIDE,

    /** Absence de concessions de l'employeur — ce n'est pas une transaction. */
    INVALIDE,

    /** Renonciation à un droit d'ordre public — clause partiellement nulle. */
    INVALIDE_PARTIELLE,

    /** Élément formel manquant — à compléter avant signature. */
    A_COMPLETER
}
