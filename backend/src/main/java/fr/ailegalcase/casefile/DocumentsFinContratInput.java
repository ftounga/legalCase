package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-DT-32-01 : input du calcul de conformité des documents de fin de contrat (FR).
 */
public record DocumentsFinContratInput(
        LocalDate dateFinContrat,
        Boolean certificatTravailRemis,
        LocalDate dateCertificatTravail,
        Boolean attestationFranceTravailRemise,
        LocalDate dateAttestationFranceTravail,
        Boolean souldeToutCompteSigne,
        LocalDate dateSouldeToutCompte,
        BigDecimal salaireMensuelBrutEur,
        LocalDate dateReference
) {

    /** Builder utilitaire — facilite la lisibilité des tests unitaires. */
    public static class Builder {
        private LocalDate dateFinContrat;
        private Boolean certificatTravailRemis;
        private LocalDate dateCertificatTravail;
        private Boolean attestationFranceTravailRemise;
        private LocalDate dateAttestationFranceTravail;
        private Boolean souldeToutCompteSigne;
        private LocalDate dateSouldeToutCompte;
        private BigDecimal salaireMensuelBrutEur;
        private LocalDate dateReference;

        public Builder dateFinContrat(LocalDate v) { this.dateFinContrat = v; return this; }
        public Builder certificatTravailRemis(Boolean v) { this.certificatTravailRemis = v; return this; }
        public Builder dateCertificatTravail(LocalDate v) { this.dateCertificatTravail = v; return this; }
        public Builder attestationFranceTravailRemise(Boolean v) { this.attestationFranceTravailRemise = v; return this; }
        public Builder dateAttestationFranceTravail(LocalDate v) { this.dateAttestationFranceTravail = v; return this; }
        public Builder souldeToutCompteSigne(Boolean v) { this.souldeToutCompteSigne = v; return this; }
        public Builder dateSouldeToutCompte(LocalDate v) { this.dateSouldeToutCompte = v; return this; }
        public Builder salaireMensuelBrutEur(BigDecimal v) { this.salaireMensuelBrutEur = v; return this; }
        public Builder dateReference(LocalDate v) { this.dateReference = v; return this; }

        public DocumentsFinContratInput build() {
            return new DocumentsFinContratInput(
                    dateFinContrat,
                    certificatTravailRemis,
                    dateCertificatTravail,
                    attestationFranceTravailRemise,
                    dateAttestationFranceTravail,
                    souldeToutCompteSigne,
                    dateSouldeToutCompte,
                    salaireMensuelBrutEur,
                    dateReference
            );
        }
    }
}
