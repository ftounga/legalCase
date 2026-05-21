package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-217-16 : requête HTTP pour les endpoints de reconnaissance d'un mariage
 * ou divorce étranger en Belgique.
 *
 * <p>Le pays cible est dérivé du workspace côté service — pas transmis dans
 * le body. Les 4 booleans talaq sont nullables sauf si
 * {@code natureActe = TALAQ_REPUDIATION} — validation portée par le Calculator
 * (réponse 400 via {@code IllegalArgumentException}).</p>
 */
public record MariageEtrangerBeReconnaissanceRequest(
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
        String commentaire
) {

    MariageEtrangerBeReconnaissanceInput toInput() {
        return new MariageEtrangerBeReconnaissanceInput(
                natureActe,
                paysOrigine,
                dateActe,
                residenceHabituelleAuMoinsUnePartie,
                nationaliteAuMoinsUnePartie,
                conformiteDroitFondPersonnel,
                conformiteFormeLocusRegitActum,
                consentementEpouse,
                epousePresente,
                procedureContradictoire,
                decisionEcriteOfficielle,
                conventionBilateraleApplicable,
                commentaire);
    }
}
