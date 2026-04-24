package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MotifGraveBeCalculatorTest {

    // Salaire mensuel de référence utilisé dans plusieurs tests.
    // 3500 / 4.33 ≈ 808.3141 €/semaine
    private static final BigDecimal SALAIRE = new BigDecimal("3500.00");

    @Test
    void compute_valid_2jAnd2j_returnsValidZeroIndemnite() {
        // Lundi 2/3/2026 → Mercredi 4/3 = 2j ouvrables ; Mercredi 4/3 → Vendredi 6/3 = 2j
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 6),
                5,
                SALAIRE);

        assertThat(r.delaiRuptureJoursOuvrables()).isEqualTo(2);
        assertThat(r.delaiMotifsJoursOuvrables()).isEqualTo(2);
        assertThat(r.motifGraveProceduralementValide()).isTrue();
        assertThat(r.indemnitePreavisSiInvalide()).isEqualByComparingTo("0.00");
        assertThat(r.indemniteManifestementDeraisonnableMin()).isEqualByComparingTo("0.00");
        assertThat(r.indemniteManifestementDeraisonnableMax()).isEqualByComparingTo("0.00");
        assertThat(r.formule()).contains("respectés");
        assertThat(r.baseJuridique()).contains("Art. 35").contains("03/07/1978");
    }

    @Test
    void compute_invalidDelaiRupture_4j_returnsFalseAndPositiveIndemnite() {
        // Lundi 2/3/2026 → Vendredi 6/3 = 4 jours ouvrables (dépassement)
        // Vendredi 6/3 → Mercredi 11/3 = lun+mar+mer = 3j ouvrables OK
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 6),
                LocalDate.of(2026, 3, 11),
                5,
                SALAIRE);

        assertThat(r.delaiRuptureJoursOuvrables()).isEqualTo(4);
        assertThat(r.delaiMotifsJoursOuvrables()).isEqualTo(3);
        assertThat(r.motifGraveProceduralementValide()).isFalse();
        // Préavis = min(5×3, 62) = 15 semaines × (3500 / 4.33) ≈ 15 × 808.3141 ≈ 12124.71 €
        assertThat(r.indemnitePreavisSiInvalide().doubleValue()).isBetween(12120.0, 12130.0);
        assertThat(r.indemniteManifestementDeraisonnableMin().doubleValue()).isBetween(2420.0, 2430.0);
        assertThat(r.indemniteManifestementDeraisonnableMax().doubleValue()).isBetween(13740.0, 13750.0);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("notification de la rupture dépassé"));
    }

    @Test
    void compute_invalidDelaiMotifs_4j_returnsFalse() {
        // Lundi 2/3/2026 → Mercredi 4/3 = 2j OK
        // Mercredi 4/3 → Mardi 10/3 = jeu+ven+lun+mar = 4j (dépassement)
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 10),
                5,
                SALAIRE);

        assertThat(r.delaiRuptureJoursOuvrables()).isEqualTo(2);
        assertThat(r.delaiMotifsJoursOuvrables()).isEqualTo(4);
        assertThat(r.motifGraveProceduralementValide()).isFalse();
        assertThat(r.indemnitePreavisSiInvalide().signum()).isPositive();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("notification des motifs"));
    }

    @Test
    void compute_limitCase_3jAnd3j_isValid() {
        // Lundi 2/3 → Jeudi 5/3 = mar+mer+jeu = 3j ouvrables (limite OK)
        // Jeudi 5/3 → Mardi 10/3 = ven+lun+mar = 3j (limite OK)
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 10),
                5,
                SALAIRE);

        assertThat(r.delaiRuptureJoursOuvrables()).isEqualTo(3);
        assertThat(r.delaiMotifsJoursOuvrables()).isEqualTo(3);
        assertThat(r.motifGraveProceduralementValide()).isTrue();
        assertThat(r.indemnitePreavisSiInvalide()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_weekendExcluded_fridayToMondayIs1j() {
        // Vendredi 6/3/2026 → Lundi 9/3 = 1j ouvrable (sa + di exclus)
        // Lundi 9/3 → Mardi 10/3 = 1j
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 6),
                LocalDate.of(2026, 3, 9),
                LocalDate.of(2026, 3, 10),
                5,
                SALAIRE);

        assertThat(r.delaiRuptureJoursOuvrables()).isEqualTo(1);
        assertThat(r.delaiMotifsJoursOuvrables()).isEqualTo(1);
        assertThat(r.motifGraveProceduralementValide()).isTrue();
    }

    @Test
    void compute_publicHolidayExcluded_1erMai2026() {
        // 1er mai 2026 = vendredi férié
        // Jeudi 30/4/2026 → Lundi 4/5/2026 : 1/5 (férié) + sa + di exclus, seul 4/5 compte = 1 jour
        // Lundi 4/5 → Mercredi 6/5 = 2j
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 5, 4),
                LocalDate.of(2026, 5, 6),
                5,
                SALAIRE);

        assertThat(r.delaiRuptureJoursOuvrables()).isEqualTo(1);
        assertThat(r.delaiMotifsJoursOuvrables()).isEqualTo(2);
        assertThat(r.motifGraveProceduralementValide()).isTrue();
    }

    @Test
    void compute_dateRuptureBeforeConnaissance_throws() {
        assertThatThrownBy(() -> MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 6),
                5,
                SALAIRE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationRupture");
    }

    @Test
    void compute_dateMotifsBeforeRupture_throws() {
        assertThatThrownBy(() -> MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 6),
                LocalDate.of(2026, 3, 4),
                5,
                SALAIRE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationMotifs");
    }

    @Test
    void compute_salaireZero_throws() {
        assertThatThrownBy(() -> MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 6),
                5,
                BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaire");
    }

    @Test
    void compute_salaireNegative_throws() {
        assertThatThrownBy(() -> MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 6),
                5,
                new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_ancienneteNegative_throws() {
        assertThatThrownBy(() -> MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 6),
                -1,
                SALAIRE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anciennette");
    }

    @Test
    void compute_nullDateConnaissance_throws() {
        assertThatThrownBy(() -> MotifGraveBeCalculator.compute(
                null,
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 6),
                5,
                SALAIRE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_ancienneteLarge_cappedAt62Weeks() {
        // Ancienneté 30 ans : 30×3 = 90 → plafonné à 62 semaines
        // 62 × (3500 / 4.33) ≈ 62 × 808.3141 ≈ 50115.47 €
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 10),  // 6j ouvrables → invalide
                LocalDate.of(2026, 3, 16),
                30,
                SALAIRE);

        assertThat(r.motifGraveProceduralementValide()).isFalse();
        assertThat(r.indemnitePreavisSiInvalide().doubleValue()).isBetween(50100.0, 50130.0);
    }

    @Test
    void compute_cct109Range_is3To17Weeks() {
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 10),
                LocalDate.of(2026, 3, 16),
                5,
                SALAIRE);

        // salaire hebdo ≈ 808.3141, min = 3 × 808.3141 ≈ 2424.94, max = 17 × 808.3141 ≈ 13741.34
        BigDecimal min = r.indemniteManifestementDeraisonnableMin();
        BigDecimal max = r.indemniteManifestementDeraisonnableMax();
        BigDecimal ratio = max.divide(min, 4, java.math.RoundingMode.HALF_UP);
        // Le ratio max/min = 17/3 ≈ 5.666...
        assertThat(ratio.doubleValue()).isBetween(5.66, 5.67);
    }

    @Test
    void compute_messagesContainArt35() {
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 10),  // invalide
                LocalDate.of(2026, 3, 16),
                5,
                SALAIRE);

        assertThat(r.messages()).isNotEmpty();
        assertThat(r.baseJuridique()).contains("03/07/1978").contains("CCT 109");
    }

    @Test
    void compute_validCase_messagesContainValidity() {
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 6),
                5,
                SALAIRE);

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m.toLowerCase()).containsAnyOf("procéduralement valable", "valable"));
    }

    @Test
    void compute_ancienneteZero_preavisIsZeroEvenIfInvalid() {
        // Ancienneté 0 : min(0×3, 62) = 0 → indemnité préavis = 0 même si invalide
        MotifGraveBeResult r = MotifGraveBeCalculator.compute(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 10),  // invalide
                LocalDate.of(2026, 3, 16),
                0,
                SALAIRE);

        assertThat(r.motifGraveProceduralementValide()).isFalse();
        assertThat(r.indemnitePreavisSiInvalide()).isEqualByComparingTo("0.00");
        // Mais l'indemnité CCT 109 reste positive
        assertThat(r.indemniteManifestementDeraisonnableMin().signum()).isPositive();
    }
}
