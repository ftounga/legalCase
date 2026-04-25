package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-16-01 : input du calcul de détection de protections + indemnité plancher 6 mois.
 *
 * <p>Tous les booléens sont nullables (interprétés comme {@code false} par défaut),
 * permettant aux clients de ne transmettre que les champs renseignés.</p>
 */
public record LicenciementNulDetectionInput(
        LocalDate dateNotificationLicenciement,
        Boolean salarieEnceinte,
        LocalDate dateAccouchement,
        Boolean salarieAccidentTravail,
        LocalDate dateConsolidationAT,
        Boolean salarieHarceleAvere,
        Boolean salarieDiscriminationAlleguee,
        Boolean salarieMotifLanceurAlerte,
        Boolean salarieMandatRepresentant,
        Boolean salarieActionJustice,
        BigDecimal salaireMensuelBrutEur,
        Integer ancienneteAnnees
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private LocalDate notif;
        private Boolean enceinte;
        private LocalDate dateAccouchement;
        private Boolean accidentTravail;
        private LocalDate dateConsolidationAT;
        private Boolean harcelementAvere;
        private Boolean discriminationAlleguee;
        private Boolean motifLanceurAlerte;
        private Boolean mandatRepresentant;
        private Boolean actionJustice;
        private BigDecimal salaire;
        private Integer anciennete;

        public Builder notif(LocalDate v) { this.notif = v; return this; }
        public Builder enceinte(Boolean v) { this.enceinte = v; return this; }
        public Builder dateAccouchement(LocalDate v) { this.dateAccouchement = v; return this; }
        public Builder accidentTravail(Boolean v) { this.accidentTravail = v; return this; }
        public Builder dateConsolidationAT(LocalDate v) { this.dateConsolidationAT = v; return this; }
        public Builder harcelementAvere(Boolean v) { this.harcelementAvere = v; return this; }
        public Builder discriminationAlleguee(Boolean v) { this.discriminationAlleguee = v; return this; }
        public Builder motifLanceurAlerte(Boolean v) { this.motifLanceurAlerte = v; return this; }
        public Builder mandatRepresentant(Boolean v) { this.mandatRepresentant = v; return this; }
        public Builder actionJustice(Boolean v) { this.actionJustice = v; return this; }
        public Builder salaire(BigDecimal v) { this.salaire = v; return this; }
        public Builder anciennete(Integer v) { this.anciennete = v; return this; }

        public LicenciementNulDetectionInput build() {
            return new LicenciementNulDetectionInput(
                    notif, enceinte, dateAccouchement,
                    accidentTravail, dateConsolidationAT,
                    harcelementAvere, discriminationAlleguee,
                    motifLanceurAlerte, mandatRepresentant, actionJustice,
                    salaire, anciennete
            );
        }
    }
}
