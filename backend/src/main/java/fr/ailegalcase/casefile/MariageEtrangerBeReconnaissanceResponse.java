package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-217-16 : réponse de l'endpoint de reconnaissance d'un mariage ou divorce
 * étranger en Belgique.
 *
 * <p>Ré-expose l'intégralité du snapshot des inputs (pour pré-remplissage /
 * ré-édition du formulaire — leçon F-DT-36) ET les sorties calculées (verdict,
 * motifs de refus, motifs de réserve, actes à produire, bases juridiques,
 * messages d'aide).</p>
 */
public record MariageEtrangerBeReconnaissanceResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage / ré-édition UI) ---
        MariageEtrangerBeReconnaissanceCalculator.NatureActeEtrangerBe natureActe,
        String paysOrigine,
        LocalDate dateActe,
        MariageEtrangerBeReconnaissanceCalculator.ResidenceHabituelleBe residenceHabituelleAuMoinsUnePartie,
        MariageEtrangerBeReconnaissanceCalculator.NationalitePartiesBe nationaliteAuMoinsUnePartie,
        Boolean conformiteDroitFondPersonnel,
        Boolean conformiteFormeLocusRegitActum,
        Boolean consentementEpouse,
        Boolean epousePresente,
        Boolean procedureContradictoire,
        Boolean decisionEcriteOfficielle,
        Boolean conventionBilateraleApplicable,
        String commentaire,
        // --- Outputs calculés ---
        MariageEtrangerBeReconnaissanceCalculator.MariageEtrangerBeReconnaissanceVerdict verdict,
        List<MariageEtrangerBeReconnaissanceCalculator.MotifReconnaissanceEtrangerBe> motifsRefus,
        List<MariageEtrangerBeReconnaissanceCalculator.MotifReconnaissanceEtrangerBe> motifsReserve,
        List<String> actesAProduire,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
