package fr.ailegalcase.casefile;

/**
 * SF-219-08 : verdict de l'outil <i>transfert d'entreprise CCT 32bis</i>
 * (transfert conventionnel BE — CCT n° 32bis du 07/06/1985 ; Loi du
 * 17/03/1965 ; Directive 2001/23/CE).
 *
 * <p>L'outil qualifie un transfert <b>conventionnel</b> (vente, fusion,
 * scission, apport d'universalité, démembrement) et vérifie la conformité
 * de la procédure (info-consult préalable, maintien automatique des
 * contrats, maintien temporaire des CCT, responsabilité solidaire 1 an).
 * Le régime de la <b>faillite</b> et de la <b>liquidation judiciaire</b>
 * n'est pas couvert par la CCT 32bis (CCT 102 et règles spécifiques)
 * — l'outil détecte ce cas et renvoie un verdict d'inéligibilité.</p>
 *
 * <ul>
 *   <li>{@link #TRANSFERT_CONFORME} — transfert qualifiant, info-consult
 *       respectée, contrats repris de plein droit avec maintien complet
 *       (ancienneté, rémunération, avantages individuels). Responsabilité
 *       solidaire cédant/cessionnaire 1 an sur dettes antérieures.</li>
 *   <li>{@link #TRANSFERT_NON_CONFORME_INFO_CONSULT} — transfert
 *       qualifiant mais info-consultation préalable des représentants
 *       (CE, DS, CPPT) non respectée — risque de sanction pénale Loi
 *       17/03/1965 + dommages-intérêts individuels. Maintien des contrats
 *       inchangé (relève de l'ordre public).</li>
 *   <li>{@link #INELIGIBLE_TYPE_OPERATION} — l'opération déclarée n'est
 *       pas un transfert conventionnel au sens CCT 32bis (faillite,
 *       liquidation judiciaire, transfert intra-groupe sans cession,
 *       cession d'actions sans transfert d'entité économique).</li>
 *   <li>{@link #INELIGIBLE_PAS_ENTITE_ECONOMIQUE} — la cession ne porte
 *       pas sur une entité économique gardant son identité après le
 *       transfert (simple cession d'actifs, externalisation d'une fonction
 *       support sans transfert de moyens) — la CCT 32bis ne s'applique
 *       pas.</li>
 *   <li>{@link #A_ANALYSER} — éléments insuffisants pour conclure
 *       automatiquement (qualification d'identité économique floue,
 *       intervention d'un avocat BE requise).</li>
 * </ul>
 */
public enum TransfertEntrepriseCct32bisVerdict {
    /** Transfert qualifiant + procédure info-consult respectée = conformité. */
    TRANSFERT_CONFORME,

    /** Transfert qualifiant mais info-consult préalable non respectée. */
    TRANSFERT_NON_CONFORME_INFO_CONSULT,

    /** Opération non qualifiante (faillite, cession d'actions, intra-groupe). */
    INELIGIBLE_TYPE_OPERATION,

    /** Pas d'entité économique préservée (cession actifs isolés). */
    INELIGIBLE_PAS_ENTITE_ECONOMIQUE,

    /** Qualification ambiguë — intervention avocat BE requise. */
    A_ANALYSER
}
