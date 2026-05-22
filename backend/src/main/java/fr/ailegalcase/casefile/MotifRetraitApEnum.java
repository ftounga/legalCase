package fr.ailegalcase.casefile;

/**
 * SF-216-11 : motif fondant la demande de retrait d'autorité parentale
 * (art. 378 et 378-1 Cciv + loi n°2022-140 du 7 février 2022 LMVSS).
 *
 * <ul>
 *   <li>{@link #CONDAMNATION_PENALE} — retrait accessoire à une condamnation
 *       pénale pour crime ou délit commis sur ou avec la personne de l'enfant
 *       (art. 378 al. 1 Cciv).</li>
 *   <li>{@link #DANGER_CARACTERISE_VIOLENCES} — mauvais traitements, dangers
 *       caractérisés mettant manifestement en danger la sécurité, la santé ou
 *       la moralité de l'enfant (art. 378-1 al. 1 Cciv).</li>
 *   <li>{@link #DESINTERET_GRAVE} — abstention volontaire d'exercer les droits
 *       et de remplir les devoirs liés à l'autorité parentale pendant plus de
 *       deux ans (art. 378-1 al. 2 Cciv).</li>
 *   <li>{@link #COMPORTEMENT_GRAVEMENT_COMPROMETTANT} — consommation habituelle
 *       et excessive d'alcool / de stupéfiants, comportement délictueux, etc.
 *       compromettant la sécurité, la santé ou la moralité de l'enfant
 *       (art. 378-1 al. 1 Cciv).</li>
 *   <li>{@link #VIOLENCES_LMVSS_2022} — violences conjugales graves en présence
 *       de l'enfant ouvrant droit au retrait accéléré (loi n°2022-140 du 7
 *       février 2022 — suspension de l'AP de plein droit puis retrait).</li>
 * </ul>
 */
public enum MotifRetraitApEnum {
    CONDAMNATION_PENALE,
    DANGER_CARACTERISE_VIOLENCES,
    DESINTERET_GRAVE,
    COMPORTEMENT_GRAVEMENT_COMPROMETTANT,
    VIOLENCES_LMVSS_2022
}
