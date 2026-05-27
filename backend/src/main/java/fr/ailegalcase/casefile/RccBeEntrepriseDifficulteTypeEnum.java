package fr.ailegalcase.casefile;

/**
 * SF-219-03 : type de reconnaissance ministérielle de l'entreprise pour
 * l'ouverture du régime dérogatoire RCC entreprise en difficulté /
 * restructuration (AR de reconnaissance + CCT sectorielle ad hoc).
 *
 * <ul>
 *   <li>{@link #EN_DIFFICULTE} — l'entreprise est reconnue en difficulté
 *       par décision du ministre de l'Emploi (situation économique
 *       grave, baisse de chiffre d'affaires, perte structurelle).</li>
 *   <li>{@link #EN_RESTRUCTURATION} — l'entreprise est reconnue en
 *       restructuration (plan agréé : licenciement collectif, fermeture
 *       de division, etc.) — la reconnaissance ouvre les âges réduits
 *       fixés par la CCT sectorielle qui accompagne le plan.</li>
 *   <li>{@link #NON_RECONNUE} — aucune procédure de reconnaissance en
 *       cours ou aboutie : le régime dérogatoire n'est <b>pas</b> ouvert
 *       (le travailleur reste éligible aux autres régimes RCC standards
 *       — général 60+/40, longue carrière 59+/40, métiers lourds 58+/35).</li>
 * </ul>
 */
public enum RccBeEntrepriseDifficulteTypeEnum {

    /** Reconnaissance ministre Emploi — entreprise en difficulté économique. */
    EN_DIFFICULTE,

    /** Reconnaissance ministre Emploi — entreprise en restructuration (plan agréé). */
    EN_RESTRUCTURATION,

    /** Aucune reconnaissance — le régime dérogatoire n'est pas ouvert. */
    NON_RECONNUE
}
