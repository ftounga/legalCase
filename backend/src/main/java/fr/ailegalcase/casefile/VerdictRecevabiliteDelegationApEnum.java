package fr.ailegalcase.casefile;

/**
 * SF-216-09 : verdict de recevabilité de la délégation d'autorité parentale
 * (art. 376-1 Cciv).
 *
 * <ul>
 *   <li>{@link #RECEVABLE_VOLONTAIRE} — délégation volontaire conjointe
 *       recevable (accord des deux parents + tiers acceptant).</li>
 *   <li>{@link #RECEVABLE_JUDICIAIRE_DESINTERET} — délégation judiciaire sur
 *       désintérêt recevable (désintérêt > 1 an).</li>
 *   <li>{@link #RECEVABLE_JUDICIAIRE_TIERS} — délégation judiciaire sur
 *       requête d'un tiers recevable (procédure JAF).</li>
 *   <li>{@link #IRRECEVABLE_ENFANT_MAJEUR} — enfant majeur : la délégation
 *       est sans objet (l'AP s'éteint à 18 ans, art. 371-1 Cciv).</li>
 *   <li>{@link #IRRECEVABLE_VOIE_INADEQUATE} — la voie demandée ne réunit pas
 *       ses conditions (ex. volontaire sans accord des deux parents).</li>
 * </ul>
 */
public enum VerdictRecevabiliteDelegationApEnum {
    RECEVABLE_VOLONTAIRE,
    RECEVABLE_JUDICIAIRE_DESINTERET,
    RECEVABLE_JUDICIAIRE_TIERS,
    IRRECEVABLE_ENFANT_MAJEUR,
    IRRECEVABLE_VOIE_INADEQUATE
}
