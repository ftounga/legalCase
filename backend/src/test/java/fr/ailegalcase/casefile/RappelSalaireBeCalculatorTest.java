package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-213-02 : tests unitaires du {@link RappelSalaireBeCalculator}.
 *
 * <p>Vérifie le calcul d'intérêts moratoires 10 % (formule, prorata, dureeAnnees
 * négative), les délais de prescription selon le type d'arriéré (PENDANT_CONTRAT
 * = 5 ans, POST_RUPTURE = 1 an, MIXTE = 1 an stricte), les statuts PRESCRIT /
 * IMMINENT / NON_PRESCRIT et la gestion des inputs invalides.</p>
 */
class RappelSalaireBeCalculatorTest {

    private static RappelSalaireBeRequest req(BigDecimal montant,
                                              LocalDate dateDebut,
                                              LocalDate dateFin,
                                              LocalDate dateRupture,
                                              LocalDate dateAction,
                                              RappelSalaireBeTypeArriereEnum type) {
        return new RappelSalaireBeRequest(montant, dateDebut, dateFin, dateRupture, dateAction, type);
    }

    // --- Intérêts moratoires ---

    @Test
    void calculate_interetsExactsSur365Jours() {
        // 5000 € × 10 % × 1 an = 500 € intérêts
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        null,
                        LocalDate.of(2025, 6, 30),
                        RappelSalaireBeTypeArriereEnum.PENDANT_CONTRAT));

        assertThat(r.interetsCourus()).isEqualByComparingTo("500.00");
        assertThat(r.totalReclame()).isEqualByComparingTo("5500.00");
        assertThat(r.tauxMoratoire()).contains("10").contains("Loi 12/04/1965").contains("art. 10");
    }

    @Test
    void calculate_interetsProratisesSur6Mois() {
        // 10 000 € × 10 % × 0,5 an ≈ 500 € (183 j / 365)
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("10000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        null,
                        LocalDate.of(2024, 12, 30), // ~183 jours après
                        RappelSalaireBeTypeArriereEnum.PENDANT_CONTRAT));

        // 10000 × 0.10 × (183/365) = 501.37
        assertThat(r.interetsCourus()).isEqualByComparingTo("501.37");
        assertThat(r.totalReclame()).isEqualByComparingTo("10501.37");
    }

    @Test
    void calculate_dateActionAvantDateFin_zeroInterets() {
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2025, 12, 31),
                        null,
                        LocalDate.of(2024, 6, 1), // antérieure à dateFin
                        RappelSalaireBeTypeArriereEnum.PENDANT_CONTRAT));

        assertThat(r.interetsCourus()).isEqualByComparingTo("0.00");
        assertThat(r.totalReclame()).isEqualByComparingTo("5000.00");
    }

    @Test
    void calculate_formuleAfficheTaux10EtMontants() {
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        null,
                        LocalDate.of(2025, 6, 30),
                        RappelSalaireBeTypeArriereEnum.PENDANT_CONTRAT));

        assertThat(r.formuleCalcul())
                .contains("5000.00")
                .contains("10%")
                .contains("500.00")
                .contains("5500.00");
    }

    // --- Prescription PENDANT_CONTRAT (5 ans) ---

    @Test
    void calculate_prescription5Ans_pendantContrat() {
        // dateFin = 2024-01-01 → prescription = 2029-01-01
        LocalDate dateFin = LocalDate.of(2024, 1, 1);
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2023, 1, 1),
                        dateFin,
                        null,
                        LocalDate.of(2025, 1, 1),
                        RappelSalaireBeTypeArriereEnum.PENDANT_CONTRAT));

        assertThat(r.dateLimitePrescription()).isEqualTo(LocalDate.of(2029, 1, 1));
        assertThat(r.statutPrescription()).isEqualTo(RappelSalaireBeStatutPrescription.NON_PRESCRIT);
    }

    @Test
    void calculate_prescritSiDelaiDepasse_pendantContrat() {
        // dateFin = 2018-01-01 → prescription = 2023-01-01 ; action 2025 → PRESCRIT
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2017, 1, 1),
                        LocalDate.of(2018, 1, 1),
                        null,
                        LocalDate.of(2025, 1, 1),
                        RappelSalaireBeTypeArriereEnum.PENDANT_CONTRAT));

        assertThat(r.statutPrescription()).isEqualTo(RappelSalaireBeStatutPrescription.PRESCRIT);
        assertThat(r.joursRestantsAvantPrescription()).isLessThanOrEqualTo(0);
    }

    // --- Prescription POST_RUPTURE (1 an depuis rupture) ---

    @Test
    void calculate_prescription1An_postRupture() {
        // dateRupture = 2024-06-30 → prescription = 2025-06-30
        LocalDate dateRupture = LocalDate.of(2024, 6, 30);
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        dateRupture,
                        LocalDate.of(2025, 1, 1),
                        RappelSalaireBeTypeArriereEnum.POST_RUPTURE));

        assertThat(r.dateLimitePrescription()).isEqualTo(LocalDate.of(2025, 6, 30));
        assertThat(r.statutPrescription()).isEqualTo(RappelSalaireBeStatutPrescription.NON_PRESCRIT);
    }

    @Test
    void calculate_prescritSiDelaiDepasse_postRupture() {
        // dateRupture 2023-01-01 → prescription 2024-01-01 ; action 2025 → PRESCRIT
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2022, 1, 1),
                        LocalDate.of(2023, 1, 1),
                        LocalDate.of(2023, 1, 1),
                        LocalDate.of(2025, 1, 1),
                        RappelSalaireBeTypeArriereEnum.POST_RUPTURE));

        assertThat(r.statutPrescription()).isEqualTo(RappelSalaireBeStatutPrescription.PRESCRIT);
    }

    // --- Statut IMMINENT (≤ 30 j) ---

    @Test
    void calculate_imminentSi15JoursAvantPrescription() {
        // dateRupture = 2024-06-30, prescription = 2025-06-30
        // dateAction = 2025-06-15 → joursRestants = 15 → IMMINENT
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        LocalDate.of(2024, 6, 30),
                        LocalDate.of(2025, 6, 15),
                        RappelSalaireBeTypeArriereEnum.POST_RUPTURE));

        assertThat(r.statutPrescription()).isEqualTo(RappelSalaireBeStatutPrescription.IMMINENT);
        assertThat(r.joursRestantsAvantPrescription()).isEqualTo(15L);
    }

    @Test
    void calculate_imminentExactementA30Jours() {
        // joursRestants = 30 (frontière incluse)
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        LocalDate.of(2024, 6, 30),
                        LocalDate.of(2025, 5, 31), // 30 j avant 2025-06-30
                        RappelSalaireBeTypeArriereEnum.POST_RUPTURE));

        assertThat(r.statutPrescription()).isEqualTo(RappelSalaireBeStatutPrescription.IMMINENT);
        assertThat(r.joursRestantsAvantPrescription()).isEqualTo(30L);
    }

    @Test
    void calculate_nonPrescritA31Jours() {
        // joursRestants = 31 → NON_PRESCRIT
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        LocalDate.of(2024, 6, 30),
                        LocalDate.of(2025, 5, 30), // 31 j avant 2025-06-30
                        RappelSalaireBeTypeArriereEnum.POST_RUPTURE));

        assertThat(r.statutPrescription()).isEqualTo(RappelSalaireBeStatutPrescription.NON_PRESCRIT);
    }

    // --- Cas MIXTE ---

    @Test
    void calculate_mixteAvecRupture_appliquePrescription1An() {
        // V1 : prescription la plus stricte (1 an post-rupture)
        LocalDate dateRupture = LocalDate.of(2024, 6, 30);
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 12, 31),
                        dateRupture,
                        LocalDate.of(2025, 1, 1),
                        RappelSalaireBeTypeArriereEnum.MIXTE));

        assertThat(r.dateLimitePrescription()).isEqualTo(LocalDate.of(2025, 6, 30));
    }

    // --- Validation des inputs ---

    @Test
    void calculate_dateFinAvantDateDebut_throws() {
        assertThatThrownBy(() -> RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 6, 30),
                        LocalDate.of(2024, 1, 1), // fin < début
                        null,
                        LocalDate.of(2025, 1, 1),
                        RappelSalaireBeTypeArriereEnum.PENDANT_CONTRAT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Période invalide");
    }

    @Test
    void calculate_postRuptureSansDateRupture_throws() {
        assertThatThrownBy(() -> RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        null, // dateRupture absente
                        LocalDate.of(2025, 1, 1),
                        RappelSalaireBeTypeArriereEnum.POST_RUPTURE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRupture")
                .hasMessageContaining("POST_RUPTURE");
    }

    @Test
    void calculate_montantNegatif_throws() {
        assertThatThrownBy(() -> RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("-1.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        null,
                        LocalDate.of(2025, 1, 1),
                        RappelSalaireBeTypeArriereEnum.PENDANT_CONTRAT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("montantBrut");
    }

    @Test
    void calculate_typeArriereNull_throws() {
        assertThatThrownBy(() -> RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        null,
                        LocalDate.of(2025, 1, 1),
                        null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeArriere");
    }

    @Test
    void calculate_requestNull_throws() {
        assertThatThrownBy(() -> RappelSalaireBeCalculator.calculate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Réponse complète ---

    @Test
    void calculate_resultat_inclueBaseJuridiqueEtAvertissement() {
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        null,
                        LocalDate.of(2025, 6, 30),
                        RappelSalaireBeTypeArriereEnum.PENDANT_CONTRAT));

        assertThat(r.baseJuridique())
                .contains("Loi 12/04/1965")
                .contains("art. 10")
                .contains("Loi 03/07/1978")
                .contains("art. 15");
        assertThat(r.avertissement()).contains("10").contains("BE");
    }

    @Test
    void calculate_dateActionEnvisageeNull_utiliseToday() {
        // Si dateActionEnvisagee est null, le calculateur utilise LocalDate.now()
        RappelSalaireBeResult r = RappelSalaireBeCalculator.calculate(
                req(new BigDecimal("5000.00"),
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 6, 30),
                        null,
                        null,
                        RappelSalaireBeTypeArriereEnum.PENDANT_CONTRAT));

        assertThat(r.dateActionEnvisagee()).isEqualTo(LocalDate.now());
    }
}
