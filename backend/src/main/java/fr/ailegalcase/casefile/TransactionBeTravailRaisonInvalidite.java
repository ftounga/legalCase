package fr.ailegalcase.casefile;

/**
 * SF-213-06 : raison précise associée à un verdict d'invalidité ou
 * d'incomplétude d'une transaction de fin de contrat en droit belge.
 *
 * <p>{@code null} pour le verdict {@link TransactionBeTravailVerdict#VALIDE}.</p>
 *
 * <ul>
 *   <li>{@link #ABSENCE_CONCESSIONS} — l'employeur n'a rien concédé
 *       (verdict {@link TransactionBeTravailVerdict#INVALIDE} ; art. 2044
 *       Cciv : le contrat n'est pas une transaction mais une quittance).</li>
 *   <li>{@link #RENONCIATION_ORDRE_PUBLIC} — renonciation à un droit
 *       d'ordre public détectée (verdict
 *       {@link TransactionBeTravailVerdict#INVALIDE_PARTIELLE} ;
 *       Loi 03/07/1978 art. 6 — dispositions impératives non
 *       susceptibles d'abdication anticipée).</li>
 *   <li>{@link #LITIGE_NON_VISE} — la transaction ne fait référence à
 *       aucun litige précis ou la checklist des renonciations expresses
 *       est vide (verdict
 *       {@link TransactionBeTravailVerdict#A_COMPLETER}).</li>
 * </ul>
 */
public enum TransactionBeTravailRaisonInvalidite {
    /** Absence de concessions réciproques de l'employeur (art. 2044 Cciv BE). */
    ABSENCE_CONCESSIONS,

    /** Renonciation à un droit d'ordre public (Loi 03/07/1978 art. 6). */
    RENONCIATION_ORDRE_PUBLIC,

    /** Litige non visé ou checklist des renonciations vide. */
    LITIGE_NON_VISE
}
