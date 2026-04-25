package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-DT-20-01 : tests unitaires du calculateur de rappel de salaire FR
 * (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail + CCN).
 */
class RappelSalaireCalculatorTest {

    private static final LocalDate DEBUT = LocalDate.of(2023, 1, 1);
    private static final LocalDate FIN = LocalDate.of(2024, 12, 31); // 24 mois inclusifs
    private static final BigDecimal DU_2500 = new BigDecimal("2500.00");
    private static final BigDecimal VERSE_2200 = new BigDecimal("2200.00");

    @Test
    void compute_nominal_24mois_300diff_avecRevalorisationEt10Pct() {
        RappelSalaireResult r = RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                "IDCC_2120", 8, null, // pas de prime CCN
                true, new BigDecimal("3.5"),
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT);

        assertThat(r.nbMoisPeriode()).isEqualTo(24);
        assertThat(r.differentielMensuelEur()).isEqualByComparingTo("300.00");
        assertThat(r.totalRappelBrutHorsRevalorisationEur()).isEqualByComparingTo("7200.00");
        assertThat(r.montantRevalorisationEur()).isEqualByComparingTo("252.00");
        assertThat(r.primeAncienneteEur()).isEqualByComparingTo("0.00");
        assertThat(r.totalRappelBrutEur()).isEqualByComparingTo("7452.00");
        assertThat(r.congesPayesSurRappelEur()).isEqualByComparingTo("745.20");
        assertThat(r.totalAvecCpEur()).isEqualByComparingTo("8197.20");
        assertThat(r.baseJuridique()).contains("L.3242-1").contains("revalorisation").contains("L.3141-26");
        assertThat(r.formule()).contains("300,00").contains("24 mois").contains("3,5").contains("CP 10");
    }

    @Test
    void compute_indexInseeFalse_pasDeRevalorisation() {
        RappelSalaireResult r = RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                null, 0, null,
                false, null,
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT);

        assertThat(r.montantRevalorisationEur()).isEqualByComparingTo("0.00");
        assertThat(r.tauxRevalorisationPct()).isNull();
        assertThat(r.totalRappelBrutEur()).isEqualByComparingTo("7200.00");
        assertThat(r.congesPayesSurRappelEur()).isEqualByComparingTo("720.00");
        assertThat(r.totalAvecCpEur()).isEqualByComparingTo("7920.00");
    }

    @Test
    void compute_methodeAucun_pasDeCp() {
        RappelSalaireResult r = RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                null, 0, null,
                false, null,
                RappelSalaireMethodeCpSurRappel.AUCUN);

        assertThat(r.congesPayesSurRappelEur()).isEqualByComparingTo("0.00");
        assertThat(r.totalAvecCpEur()).isEqualByComparingTo(r.totalRappelBrutEur());
        assertThat(r.baseJuridique()).doesNotContain("L.3141-26");
        assertThat(r.messages()).anyMatch(m -> m.contains("Aucune ICCP"));
    }

    @Test
    void compute_methodeMaintien_environ1Sur12() {
        // Brut = 7200 → 7200 / 12 = 600.00
        RappelSalaireResult r = RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                null, 0, null,
                false, null,
                RappelSalaireMethodeCpSurRappel.MAINTIEN);

        assertThat(r.congesPayesSurRappelEur()).isEqualByComparingTo("600.00");
        assertThat(r.totalAvecCpEur()).isEqualByComparingTo("7800.00");
    }

    @Test
    void compute_ccnInconnue_primeZero() {
        RappelSalaireResult r = RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                "IDCC_INCONNU", 8, null,
                false, null,
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT);

        assertThat(r.primeAncienneteEur()).isEqualByComparingTo("0.00");
        assertThat(r.messages()).anyMatch(m -> m.contains("aucune tranche") || m.contains("CCN inconnue"));
    }

    @Test
    void compute_ccnConnueAvecPrime_appliquePourcentage() {
        // Prime = 5% du brut hors revalo = 7200 × 0.05 = 360.00
        RappelSalaireResult r = RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                "IDCC_2120", 8, new BigDecimal("5"),
                false, null,
                RappelSalaireMethodeCpSurRappel.AUCUN);

        assertThat(r.primeAncienneteEur()).isEqualByComparingTo("360.00");
        assertThat(r.totalRappelBrutEur()).isEqualByComparingTo("7560.00");
        assertThat(r.baseJuridique()).contains("CCN").contains("IDCC_2120");
        assertThat(r.messages()).anyMatch(m -> m.contains("5") && m.contains("IDCC_2120"));
    }

    @Test
    void compute_periodeFinAvantDebut_throwsIllegalArgument() {
        assertThatThrownBy(() -> RappelSalaireCalculator.compute(
                FIN, DEBUT, DU_2500, VERSE_2200,
                null, 0, null, false, null,
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Période");
    }

    @Test
    void compute_duInferieurAVerse_throwsIllegalArgument() {
        assertThatThrownBy(() -> RappelSalaireCalculator.compute(
                DEBUT, FIN, VERSE_2200, DU_2500,
                null, 0, null, false, null,
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("différentiel");
    }

    @Test
    void compute_duEgalVerse_throwsIllegalArgument() {
        assertThatThrownBy(() -> RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, DU_2500,
                null, 0, null, false, null,
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("différentiel");
    }

    @Test
    void compute_montantDuZero_throwsIllegalArgument() {
        assertThatThrownBy(() -> RappelSalaireCalculator.compute(
                DEBUT, FIN, BigDecimal.ZERO, VERSE_2200,
                null, 0, null, false, null,
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dû mensuel");
    }

    @Test
    void compute_versNegatif_throwsIllegalArgument() {
        assertThatThrownBy(() -> RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, new BigDecimal("-100.00"),
                null, 0, null, false, null,
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("perçu");
    }

    @Test
    void compute_tauxNegatif_throwsIllegalArgument() {
        assertThatThrownBy(() -> RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                null, 0, null,
                true, new BigDecimal("-1.0"),
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revalorisation");
    }

    @Test
    void compute_tauxSuperieur100_throwsIllegalArgument() {
        assertThatThrownBy(() -> RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                null, 0, null,
                true, new BigDecimal("150.0"),
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revalorisation");
    }

    @Test
    void compute_methodeNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                null, 0, null, false, null,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Méthode CP");
    }

    @Test
    void compute_indexInseeTrueSansTaux_throwsIllegalArgument() {
        assertThatThrownBy(() -> RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                null, 0, null,
                true, null,
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Taux");
    }

    @Test
    void compute_arrondiCentimeHALFUP() {
        // Différentiel 333.335 × 1 mois = 333.34 (HALF_UP).
        RappelSalaireResult r = RappelSalaireCalculator.compute(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
                new BigDecimal("2500.005"), new BigDecimal("2166.67"),
                null, 0, null,
                false, null,
                RappelSalaireMethodeCpSurRappel.AUCUN);

        // 2500.01 - 2166.67 = 333.34
        assertThat(r.differentielMensuelEur()).isEqualByComparingTo("333.34");
        assertThat(r.nbMoisPeriode()).isEqualTo(1);
        assertThat(r.totalRappelBrutEur()).isEqualByComparingTo("333.34");
    }

    @Test
    void compute_messagesContiennentPrescription3Ans() {
        RappelSalaireResult r = RappelSalaireCalculator.compute(
                DEBUT, FIN, DU_2500, VERSE_2200,
                null, 0, null,
                false, null,
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT);

        assertThat(r.messages()).anyMatch(m -> m.contains("Prescription") && m.contains("3 ans"));
        assertThat(r.messages()).anyMatch(m -> m.contains("L.3245-1"));
    }

    @Test
    void compute_periodeLongue_messagePrescriptionPartielle() {
        RappelSalaireResult r = RappelSalaireCalculator.compute(
                LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31),
                DU_2500, VERSE_2200,
                null, 0, null,
                false, null,
                RappelSalaireMethodeCpSurRappel.DIX_POURCENT);

        assertThat(r.nbMoisPeriode()).isEqualTo(60);
        assertThat(r.messages()).anyMatch(m -> m.contains("36 mois") || m.contains("prescription partielle"));
    }
}
