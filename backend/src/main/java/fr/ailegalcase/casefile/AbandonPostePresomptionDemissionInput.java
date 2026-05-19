package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-206-01 : input du calcul d'analyse de contestation d'une présomption de
 * démission par abandon de poste (FRANCE).
 *
 * <p>Les booléens sont nullables (interprétés à défaut comme {@code null} —
 * pas comme false par défaut, car l'absence d'information ne doit pas faire
 * détecter d'irrégularité). Le pays est dérivé du workspace côté service.</p>
 */
public record AbandonPostePresomptionDemissionInput(
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

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private LocalDate dateMiseEnDemeurePresentee;
        private AbandonPostePresomptionDemissionCalculator.ModeNotification modeNotificationMiseEnDemeure;
        private Integer delaiAccordeJours;
        private Boolean miseEnDemeureMentionneDelai;
        private Boolean miseEnDemeureMentionneConsequences;
        private AbandonPostePresomptionDemissionCalculator.MotifAbsence motifAbsenceInvoque;
        private String motifAbsenceCommentaire;
        private Boolean repriseOuJustificationDansDelai;
        private LocalDate dateRepriseOuJustification;

        public Builder dateMiseEnDemeurePresentee(LocalDate v) { this.dateMiseEnDemeurePresentee = v; return this; }
        public Builder modeNotificationMiseEnDemeure(AbandonPostePresomptionDemissionCalculator.ModeNotification v) { this.modeNotificationMiseEnDemeure = v; return this; }
        public Builder delaiAccordeJours(Integer v) { this.delaiAccordeJours = v; return this; }
        public Builder miseEnDemeureMentionneDelai(Boolean v) { this.miseEnDemeureMentionneDelai = v; return this; }
        public Builder miseEnDemeureMentionneConsequences(Boolean v) { this.miseEnDemeureMentionneConsequences = v; return this; }
        public Builder motifAbsenceInvoque(AbandonPostePresomptionDemissionCalculator.MotifAbsence v) { this.motifAbsenceInvoque = v; return this; }
        public Builder motifAbsenceCommentaire(String v) { this.motifAbsenceCommentaire = v; return this; }
        public Builder repriseOuJustificationDansDelai(Boolean v) { this.repriseOuJustificationDansDelai = v; return this; }
        public Builder dateRepriseOuJustification(LocalDate v) { this.dateRepriseOuJustification = v; return this; }

        public AbandonPostePresomptionDemissionInput build() {
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
}
