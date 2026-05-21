package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-217-16 : input pour l'outil décisionnel BE de reconnaissance d'un mariage
 * ou divorce étranger en Belgique (incluant le talaq).
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.</p>
 *
 * <p>Les 4 booleans talaq ({@code consentementEpouse}, {@code epousePresente},
 * {@code procedureContradictoire}, {@code decisionEcriteOfficielle}) sont
 * obligatoires uniquement si {@code natureActe = TALAQ_REPUDIATION} —
 * validation portée par le Calculator.</p>
 */
public record MariageEtrangerBeReconnaissanceInput(
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

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private MariageEtrangerBeReconnaissanceCalculator.NatureActeEtrangerBe natureActe;
        private String paysOrigine;
        private LocalDate dateActe;
        private MariageEtrangerBeReconnaissanceCalculator.ResidenceHabituelleBe residenceHabituelleAuMoinsUnePartie;
        private MariageEtrangerBeReconnaissanceCalculator.NationalitePartiesBe nationaliteAuMoinsUnePartie;
        private Boolean conformiteDroitFondPersonnel;
        private Boolean conformiteFormeLocusRegitActum;
        private Boolean consentementEpouse;
        private Boolean epousePresente;
        private Boolean procedureContradictoire;
        private Boolean decisionEcriteOfficielle;
        private Boolean conventionBilateraleApplicable;
        private String commentaire;

        public Builder natureActe(MariageEtrangerBeReconnaissanceCalculator.NatureActeEtrangerBe v) {
            this.natureActe = v; return this;
        }
        public Builder paysOrigine(String v) { this.paysOrigine = v; return this; }
        public Builder dateActe(LocalDate v) { this.dateActe = v; return this; }
        public Builder residenceHabituelleAuMoinsUnePartie(
                MariageEtrangerBeReconnaissanceCalculator.ResidenceHabituelleBe v) {
            this.residenceHabituelleAuMoinsUnePartie = v; return this;
        }
        public Builder nationaliteAuMoinsUnePartie(
                MariageEtrangerBeReconnaissanceCalculator.NationalitePartiesBe v) {
            this.nationaliteAuMoinsUnePartie = v; return this;
        }
        public Builder conformiteDroitFondPersonnel(Boolean v) { this.conformiteDroitFondPersonnel = v; return this; }
        public Builder conformiteFormeLocusRegitActum(Boolean v) { this.conformiteFormeLocusRegitActum = v; return this; }
        public Builder consentementEpouse(Boolean v) { this.consentementEpouse = v; return this; }
        public Builder epousePresente(Boolean v) { this.epousePresente = v; return this; }
        public Builder procedureContradictoire(Boolean v) { this.procedureContradictoire = v; return this; }
        public Builder decisionEcriteOfficielle(Boolean v) { this.decisionEcriteOfficielle = v; return this; }
        public Builder conventionBilateraleApplicable(Boolean v) { this.conventionBilateraleApplicable = v; return this; }
        public Builder commentaire(String v) { this.commentaire = v; return this; }

        public MariageEtrangerBeReconnaissanceInput build() {
            return new MariageEtrangerBeReconnaissanceInput(
                    natureActe, paysOrigine, dateActe,
                    residenceHabituelleAuMoinsUnePartie, nationaliteAuMoinsUnePartie,
                    conformiteDroitFondPersonnel, conformiteFormeLocusRegitActum,
                    consentementEpouse, epousePresente,
                    procedureContradictoire, decisionEcriteOfficielle,
                    conventionBilateraleApplicable, commentaire);
        }
    }
}
