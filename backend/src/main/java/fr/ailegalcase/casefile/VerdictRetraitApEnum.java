package fr.ailegalcase.casefile;

/**
 * SF-216-11 : verdict de recevabilité de la demande de retrait d'autorité
 * parentale (art. 378-381 Cciv + loi 2/3/2022 LMVSS).
 *
 * <ul>
 *   <li>{@link #RETRAIT_PLEIN_DROIT} — retrait accessoire à une condamnation
 *       pénale pour crime sur l'enfant (art. 378 al. 1) — prononcé de plein
 *       droit par la juridiction pénale.</li>
 *   <li>{@link #RETRAIT_CIVIL_JAF} — retrait civil saisine JAF / tribunal
 *       judiciaire (art. 378-1 Cciv).</li>
 *   <li>{@link #SUSPENSION_ACCELEREE_LMVSS_2022} — suspension automatique de
 *       l'AP + saisine accélérée pour retrait dans le cadre des violences
 *       conjugales graves loi 2022-140.</li>
 *   <li>{@link #IRRECEVABLE_ENFANT_MAJEUR} — l'enfant est majeur, l'AP s'est
 *       éteinte (art. 371-1 Cciv) — la demande est sans objet.</li>
 *   <li>{@link #IRRECEVABLE_MOTIF_NON_CARACTERISE} — le motif allégué n'est
 *       pas factuellement étayé par les pièces (ex. désintérêt invoqué sans
 *       documentation des deux ans).</li>
 * </ul>
 */
public enum VerdictRetraitApEnum {
    RETRAIT_PLEIN_DROIT,
    RETRAIT_CIVIL_JAF,
    SUSPENSION_ACCELEREE_LMVSS_2022,
    IRRECEVABLE_ENFANT_MAJEUR,
    IRRECEVABLE_MOTIF_NON_CARACTERISE
}
