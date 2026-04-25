package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.DocumentsFinContratCalculator.VerdictRisqueContentieux;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentsFinContratCalculatorTest {

    private static final LocalDate FIN = LocalDate.of(2026, 4, 15);
    private static final BigDecimal SALAIRE = new BigDecimal("3000.00");
    /** Date de référence stable pour les tests : 2 mois après la fin de contrat. */
    private static final LocalDate REF = LocalDate.of(2026, 6, 15);

    private DocumentsFinContratInput.Builder baseBuilder() {
        return new DocumentsFinContratInput.Builder()
                .dateFinContrat(FIN)
                .salaireMensuelBrutEur(SALAIRE)
                .dateReference(REF);
    }

    // --- Conformité parfaite ---

    @Test
    void compute_troisDocumentsParfaits_score100Faible() {
        var input = baseBuilder()
                .certificatTravailRemis(true).dateCertificatTravail(FIN.plusDays(2))
                .attestationFranceTravailRemise(true).dateAttestationFranceTravail(FIN.plusDays(3))
                .souldeToutCompteSigne(true).dateSouldeToutCompte(FIN.plusDays(5))
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        assertThat(r.scoreConformiteEmployeur()).isEqualTo(100);
        assertThat(r.verdictRisqueContentieux()).isEqualTo(VerdictRisqueContentieux.FAIBLE);
        assertThat(r.certificatRemisDansDelai()).isTrue();
        assertThat(r.attestationRemiseDansDelai()).isTrue();
        assertThat(r.souldeToutCompteValide()).isTrue();
        assertThat(r.totalSanctionsCumulables()).isZero();
        assertThat(r.indemniteRetardCertificatEur()).isEqualByComparingTo("0.00");
        assertThat(r.indemniteRetardAttestationEur()).isEqualByComparingTo("0.00");
    }

    // --- Combinaisons partielles ---

    @Test
    void compute_certificatSeulOK_score35Eleve() {
        var input = baseBuilder()
                .certificatTravailRemis(true).dateCertificatTravail(FIN.plusDays(2))
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        // Certificat 35 + STC non signé donc +30 = 65 ?
        // Wait: stcSigne false → score STC OK +30 ; attest pas remis → -35
        // Donc 35 (certif) + 30 (STC absent) = 65 = MOYEN
        assertThat(r.scoreConformiteEmployeur()).isEqualTo(65);
        assertThat(r.verdictRisqueContentieux()).isEqualTo(VerdictRisqueContentieux.MOYEN);
        assertThat(r.certificatRemisDansDelai()).isTrue();
        assertThat(r.attestationRemiseDansDelai()).isFalse();
        assertThat(r.souldeToutCompteValide()).isFalse();
    }

    @Test
    void compute_certificatEtAttestOK_stcAbsent_score100Faible() {
        var input = baseBuilder()
                .certificatTravailRemis(true).dateCertificatTravail(FIN)
                .attestationFranceTravailRemise(true).dateAttestationFranceTravail(FIN)
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        // 35 + 35 + 30 (STC absent → employeur non en faute) = 100
        assertThat(r.scoreConformiteEmployeur()).isEqualTo(100);
        assertThat(r.verdictRisqueContentieux()).isEqualTo(VerdictRisqueContentieux.FAIBLE);
    }

    @Test
    void compute_aucunDocumentRemis_score30Eleve() {
        var input = baseBuilder().build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        // 0 + 0 + 30 (STC non signé donc pas d'erreur) = 30 ELEVE
        assertThat(r.scoreConformiteEmployeur()).isEqualTo(30);
        assertThat(r.verdictRisqueContentieux()).isEqualTo(VerdictRisqueContentieux.ELEVE);
        assertThat(r.totalSanctionsCumulables()).isEqualTo(2);
    }

    // --- Délais et indemnités de retard ---

    @Test
    void compute_certificatHorsDelai10jours_indemniteCalculee() {
        // Fin = 15/04, certificat remis 25/04 = 10j après → 3 jours de retard (au-delà 7j)
        var input = baseBuilder()
                .certificatTravailRemis(true).dateCertificatTravail(FIN.plusDays(10))
                .attestationFranceTravailRemise(true).dateAttestationFranceTravail(FIN)
                .souldeToutCompteSigne(true).dateSouldeToutCompte(FIN.plusDays(5))
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        // 3 jours × (3000 / 30) = 300 €
        assertThat(r.indemniteRetardCertificatEur()).isEqualByComparingTo("300.00");
        assertThat(r.certificatRemisDansDelai()).isFalse();
    }

    @Test
    void compute_certificatNonRemis_indemniteJusquaToday() {
        // Fin = 15/04, ref = 15/06, donc retard depuis 22/04 jusqu'à 15/06 = 54 jours
        var input = baseBuilder()
                .attestationFranceTravailRemise(true).dateAttestationFranceTravail(FIN)
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        assertThat(r.certificatRemisDansDelai()).isFalse();
        // 54 jours × 100 = 5400 €
        assertThat(r.indemniteRetardCertificatEur()).isEqualByComparingTo("5400.00");
    }

    @Test
    void compute_certificatRemisExactementJ7_dansDelai() {
        var input = baseBuilder()
                .certificatTravailRemis(true).dateCertificatTravail(FIN.plusDays(7))
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        assertThat(r.certificatRemisDansDelai()).isTrue();
        assertThat(r.indemniteRetardCertificatEur()).isEqualByComparingTo("0.00");
    }

    // --- STC ---

    @Test
    void compute_stcSigneDans30j_valide() {
        var input = baseBuilder()
                .souldeToutCompteSigne(true).dateSouldeToutCompte(FIN.plusDays(20))
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        assertThat(r.souldeToutCompteValide()).isTrue();
    }

    @Test
    void compute_stcSigne45j_invalide() {
        var input = baseBuilder()
                .souldeToutCompteSigne(true).dateSouldeToutCompte(FIN.plusDays(45))
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        assertThat(r.souldeToutCompteValide()).isFalse();
    }

    @Test
    void compute_stcSigneIlYa3Mois_contestable() {
        // Ref = 15/06, signature il y a 3 mois = 15/03 (avant fin) → impossible cas réel
        // Adapt : dateRef = 1 an après fin
        var input = baseBuilder()
                .dateReference(FIN.plusMonths(3))
                .souldeToutCompteSigne(true).dateSouldeToutCompte(FIN.plusDays(5))
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        // Signé 20/04, ref 15/07 → 2 mois 25 jours après → encore < 6 mois → contestable
        assertThat(r.souldeToutCompteContestable()).isTrue();
    }

    @Test
    void compute_stcSigneIlYa8Mois_nonContestable() {
        var input = baseBuilder()
                .dateReference(FIN.plusMonths(8))
                .souldeToutCompteSigne(true).dateSouldeToutCompte(FIN.plusDays(5))
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        // Signé 20/04, ref 15/12 → ~7 mois 25 jours après signature → > 6 mois → non contestable
        assertThat(r.souldeToutCompteContestable()).isFalse();
    }

    // --- Base juridique + formule ---

    @Test
    void compute_baseJuridiqueIncludesAllArticles() {
        var input = baseBuilder().build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        assertThat(r.baseJuridique())
                .contains("L.1234-19")
                .contains("R.1234-9")
                .contains("L.1234-20");
    }

    @Test
    void compute_formuleIsHumanReadable() {
        var input = baseBuilder()
                .certificatTravailRemis(true).dateCertificatTravail(FIN)
                .attestationFranceTravailRemise(true).dateAttestationFranceTravail(FIN)
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        assertThat(r.formule())
                .contains("certificat")
                .contains("attestation")
                .contains("STC")
                .contains("score")
                .contains("FAIBLE");
    }

    @Test
    void compute_messagesContentieuxPresentSiSanction() {
        var input = baseBuilder()
                .certificatTravailRemis(true).dateCertificatTravail(FIN.plusDays(20))
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        assertThat(r.messagesContentieux()).isNotEmpty();
        assertThat(r.totalSanctionsCumulables()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void compute_messagesContentieuxVidesSiToutOK() {
        var input = baseBuilder()
                .certificatTravailRemis(true).dateCertificatTravail(FIN.plusDays(2))
                .attestationFranceTravailRemise(true).dateAttestationFranceTravail(FIN.plusDays(3))
                .souldeToutCompteSigne(true).dateSouldeToutCompte(FIN.plusDays(5))
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        assertThat(r.messagesContentieux()).isEmpty();
    }

    // --- Cas d'erreur ---

    @Test
    void compute_dateFinContratNull_throws() {
        var input = baseBuilder().dateFinContrat(null).build();
        assertThatThrownBy(() -> DocumentsFinContratCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fin de contrat");
    }

    @Test
    void compute_salaireZero_throws() {
        var input = baseBuilder().salaireMensuelBrutEur(BigDecimal.ZERO).build();
        assertThatThrownBy(() -> DocumentsFinContratCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    void compute_salaireNull_throws() {
        var input = baseBuilder().salaireMensuelBrutEur(null).build();
        assertThatThrownBy(() -> DocumentsFinContratCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_paysBELGIQUE_throws() {
        var input = baseBuilder().build();
        assertThatThrownBy(() -> DocumentsFinContratCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void compute_paysNull_throws() {
        var input = baseBuilder().build();
        assertThatThrownBy(() -> DocumentsFinContratCalculator.compute(input, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_inputNull_throws() {
        assertThatThrownBy(() -> DocumentsFinContratCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_countryReturnedFRANCE() {
        var input = baseBuilder().build();
        var r = DocumentsFinContratCalculator.compute(input, "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void compute_attestationNonRemise_indemniteCalculee() {
        var input = baseBuilder()
                .certificatTravailRemis(true).dateCertificatTravail(FIN)
                .build();
        var r = DocumentsFinContratCalculator.compute(input, "FRANCE");
        assertThat(r.attestationRemiseDansDelai()).isFalse();
        // Retard depuis 22/04 jusqu'à 15/06 = 54 jours × 100 = 5400 €
        assertThat(r.indemniteRetardAttestationEur()).isEqualByComparingTo("5400.00");
    }
}
