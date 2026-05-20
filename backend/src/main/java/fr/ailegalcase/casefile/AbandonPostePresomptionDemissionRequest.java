package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-206-01 : requête HTTP pour l'endpoint d'analyse de contestation d'une
 * présomption de démission par abandon de poste (FRANCE).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis dans le body.</p>
 */
public record AbandonPostePresomptionDemissionRequest(
        LocalDate dateMiseEnDemeurePresentee,
        AbandonPostePresomptionDemissionCalculator.ModeNotification modeNotificationMiseEnDemeure,
        Integer delaiAccordeJours,
        Boolean miseEnDemeureMentionneDelai,
        Boolean miseEnDemeureMentionneConsequences,
        AbandonPostePresomptionDemissionCalculator.MotifAbsence motifAbsenceInvoque,
        String motifAbsenceCommentaire,
        Boolean repriseOuJustificationDansDelai,
        LocalDate dateRepriseOuJustification
) {

    AbandonPostePresomptionDemissionInput toInput() {
        return new AbandonPostePresomptionDemissionInput(
                dateMiseEnDemeurePresentee,
                modeNotificationMiseEnDemeure,
                delaiAccordeJours,
                miseEnDemeureMentionneDelai,
                miseEnDemeureMentionneConsequences,
                motifAbsenceInvoque,
                motifAbsenceCommentaire,
                repriseOuJustificationDansDelai,
                dateRepriseOuJustification
        );
    }
}
