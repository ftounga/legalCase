package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-217-16 : résultat structuré du moteur décisionnel BE de reconnaissance
 * d'un mariage ou divorce étranger en Belgique (incluant le talaq).
 *
 * <p>Les listes peuvent être vides selon le verdict :</p>
 * <ul>
 *   <li>{@code motifsRefus} non vide uniquement pour
 *       {@link MariageEtrangerBeReconnaissanceCalculator.MariageEtrangerBeReconnaissanceVerdict#RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC} ;</li>
 *   <li>{@code motifsReserve} non vide pour
 *       {@link MariageEtrangerBeReconnaissanceCalculator.MariageEtrangerBeReconnaissanceVerdict#RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS} ;</li>
 *   <li>{@code actesAProduire} non vide quel que soit le verdict (au minimum
 *       un message d'aide).</li>
 * </ul>
 */
public record MariageEtrangerBeReconnaissanceResult(
        MariageEtrangerBeReconnaissanceCalculator.MariageEtrangerBeReconnaissanceVerdict verdict,
        List<MariageEtrangerBeReconnaissanceCalculator.MotifReconnaissanceEtrangerBe> motifsRefus,
        List<MariageEtrangerBeReconnaissanceCalculator.MotifReconnaissanceEtrangerBe> motifsReserve,
        List<String> actesAProduire,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
