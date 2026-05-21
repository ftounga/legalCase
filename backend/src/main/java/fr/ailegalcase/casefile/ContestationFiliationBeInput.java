package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-217-18 : input pour l'outil décisionnel BE d'analyse de recevabilité d'une
 * action en contestation de filiation paternelle.
 *
 * <p>Le pays est dérivé du workspace côté service — pas transmis ici.</p>
 */
public record ContestationFiliationBeInput(
        ContestationFiliationBeCalculator.NatureActionFiliationBe natureActionFiliation,
        ContestationFiliationBeCalculator.QualiteDemandeurFiliationBe qualiteDemandeur,
        LocalDate dateNaissanceEnfant,
        LocalDate dateConnaissanceFaitContestation,
        boolean possessionEtatConforme,
        int dureePossessionEtatAnnees,
        boolean expertiseAdnDisponible,
        boolean demandeExpertiseAdnEnvisagee,
        String commentaire
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private ContestationFiliationBeCalculator.NatureActionFiliationBe natureActionFiliation;
        private ContestationFiliationBeCalculator.QualiteDemandeurFiliationBe qualiteDemandeur;
        private LocalDate dateNaissanceEnfant;
        private LocalDate dateConnaissanceFaitContestation;
        private boolean possessionEtatConforme = false;
        private int dureePossessionEtatAnnees = 0;
        private boolean expertiseAdnDisponible = false;
        private boolean demandeExpertiseAdnEnvisagee = false;
        private String commentaire;

        public Builder natureActionFiliation(ContestationFiliationBeCalculator.NatureActionFiliationBe v) { this.natureActionFiliation = v; return this; }
        public Builder qualiteDemandeur(ContestationFiliationBeCalculator.QualiteDemandeurFiliationBe v) { this.qualiteDemandeur = v; return this; }
        public Builder dateNaissanceEnfant(LocalDate v) { this.dateNaissanceEnfant = v; return this; }
        public Builder dateConnaissanceFaitContestation(LocalDate v) { this.dateConnaissanceFaitContestation = v; return this; }
        public Builder possessionEtatConforme(boolean v) { this.possessionEtatConforme = v; return this; }
        public Builder dureePossessionEtatAnnees(int v) { this.dureePossessionEtatAnnees = v; return this; }
        public Builder expertiseAdnDisponible(boolean v) { this.expertiseAdnDisponible = v; return this; }
        public Builder demandeExpertiseAdnEnvisagee(boolean v) { this.demandeExpertiseAdnEnvisagee = v; return this; }
        public Builder commentaire(String v) { this.commentaire = v; return this; }

        public ContestationFiliationBeInput build() {
            return new ContestationFiliationBeInput(
                    natureActionFiliation, qualiteDemandeur,
                    dateNaissanceEnfant, dateConnaissanceFaitContestation,
                    possessionEtatConforme, dureePossessionEtatAnnees,
                    expertiseAdnDisponible, demandeExpertiseAdnEnvisagee,
                    commentaire);
        }
    }
}
