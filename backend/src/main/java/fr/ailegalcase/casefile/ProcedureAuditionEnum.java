package fr.ailegalcase.casefile;

/**
 * SF-216-13 : type de procédure civile dans laquelle l'audition du mineur
 * est sollicitée (art. 388-1 Cciv).
 *
 * <ul>
 *   <li>{@link #DIVORCE} — procédure de divorce ou séparation de corps.</li>
 *   <li>{@link #AUTORITE_PARENTALE} — procédure portant sur l'exercice de
 *       l'autorité parentale (résidence, droit de visite, scolarité).</li>
 *   <li>{@link #GARDE} — modalités de garde / résidence de l'enfant.</li>
 *   <li>{@link #SUCCESSION} — procédure successorale concernant l'enfant
 *       (rare — partage, recel, recours).</li>
 *   <li>{@link #AUTRE} — toute autre procédure civile concernant le mineur
 *       (adoption, filiation, changement de nom, etc.).</li>
 * </ul>
 */
public enum ProcedureAuditionEnum {
    DIVORCE,
    AUTORITE_PARENTALE,
    GARDE,
    SUCCESSION,
    AUTRE
}
