package fr.ailegalcase.casefile;

/**
 * SF-219-32 : mode de notification de la demande de congé parental BE
 * — AR du 29/10/1997 art. 6.
 *
 * <p>L'art. 6 impose un formalisme strict : la notification doit être
 * faite par <b>lettre recommandée</b> avec accusé de réception
 * <i>ou</i> par <b>remise en main propre</b> contre accusé daté et
 * signé. Tout autre mode (e-mail simple, SMS, communication orale)
 * fragilise la preuve de la notification et expose à un refus
 * d'autorisation par l'employeur ou à un litige sur la date de
 * dépôt.</p>
 *
 * <ul>
 *   <li>{@link #LETTRE_RECOMMANDEE} — voie privilégiée et la plus
 *       sûre (date certaine de dépôt = date de présentation).</li>
 *   <li>{@link #REMISE_EN_MAIN} — admise sous réserve d'un accusé de
 *       réception daté et signé par l'employeur (recommandation
 *       jurisprudence Cass. BE 26/05/2008).</li>
 *   <li>{@link #AUTRE} — mode non conforme art. 6 AR 29/10/1997 ;
 *       expose à un verdict INELIGIBLE_FORMALISME.</li>
 * </ul>
 */
public enum InterruptionCarriereSoinsParentalModeNotification {
    /** Lettre recommandée — voie standard art. 6 AR 29/10/1997. */
    LETTRE_RECOMMANDEE,

    /** Remise en main propre contre accusé daté signé. */
    REMISE_EN_MAIN,

    /** Mode non conforme — fragilise la preuve de notification. */
    AUTRE
}
