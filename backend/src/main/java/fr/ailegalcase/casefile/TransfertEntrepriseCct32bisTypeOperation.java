package fr.ailegalcase.casefile;

/**
 * SF-219-08 : type d'opération de transfert d'entreprise déclaré par
 * l'avocat — détermine l'applicabilité de la CCT n° 32bis du 07/06/1985.
 *
 * <p>Seuls les transferts <b>conventionnels</b> (Convention librement
 * conclue entre cédant et cessionnaire, postérieurs au 01/04/1985 selon
 * la CCT 32bis art. 1) entrent dans le champ. Les transferts par
 * <b>faillite</b> ou <b>liquidation judiciaire</b> relèvent de régimes
 * spécifiques (CCT 102) où le maintien automatique des contrats est
 * écarté. Les <b>cessions d'actions</b> ne sont pas des transferts au
 * sens CCT 32bis (la personne morale employeur ne change pas — pas de
 * cession d'entité).</p>
 *
 * <ul>
 *   <li>{@link #VENTE_FONDS_COMMERCE} — vente du fonds de commerce ou
 *       d'une universalité : qualifie pleinement si entité économique
 *       gardée.</li>
 *   <li>{@link #FUSION_ABSORPTION} — fusion par absorption (Code des
 *       sociétés et associations) : qualifie pleinement.</li>
 *   <li>{@link #SCISSION} — scission totale ou partielle de société :
 *       qualifie pour les branches transférées.</li>
 *   <li>{@link #APPORT_UNIVERSALITE} — apport d'une branche d'activité ou
 *       d'une universalité : qualifie si entité économique gardée.</li>
 *   <li>{@link #DEMEMBREMENT} — démembrement ou réorganisation
 *       conventionnelle : qualifie selon l'analyse d'entité économique.</li>
 *   <li>{@link #FAILLITE} — faillite judiciaire : <b>non qualifiant</b>
 *       CCT 32bis (régime CCT 102, maintien des contrats écarté).</li>
 *   <li>{@link #LIQUIDATION_JUDICIAIRE} — liquidation judiciaire : <b>non
 *       qualifiant</b> CCT 32bis.</li>
 *   <li>{@link #CESSION_ACTIONS} — cession d'actions / parts : <b>non
 *       qualifiant</b> — la personne morale employeur reste la même, pas
 *       de transfert d'entité au sens CCT 32bis.</li>
 *   <li>{@link #TRANSFERT_INTRA_GROUPE_SANS_CESSION} — réorganisation
 *       intra-groupe sans cession véritable : <b>non qualifiant</b>.</li>
 * </ul>
 */
public enum TransfertEntrepriseCct32bisTypeOperation {
    /** Vente du fonds de commerce ou d'une universalité — qualifiant. */
    VENTE_FONDS_COMMERCE,

    /** Fusion par absorption (CSA) — qualifiant. */
    FUSION_ABSORPTION,

    /** Scission totale ou partielle de société — qualifiant. */
    SCISSION,

    /** Apport d'une branche d'activité ou universalité — qualifiant. */
    APPORT_UNIVERSALITE,

    /** Démembrement / réorganisation conventionnelle — qualifiant si entité gardée. */
    DEMEMBREMENT,

    /** Faillite judiciaire — non qualifiant CCT 32bis (régime CCT 102). */
    FAILLITE,

    /** Liquidation judiciaire — non qualifiant CCT 32bis. */
    LIQUIDATION_JUDICIAIRE,

    /** Cession d'actions / parts — non qualifiant (l'employeur ne change pas). */
    CESSION_ACTIONS,

    /** Transfert intra-groupe sans véritable cession — non qualifiant. */
    TRANSFERT_INTRA_GROUPE_SANS_CESSION
}
