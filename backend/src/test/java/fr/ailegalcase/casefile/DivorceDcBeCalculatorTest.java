package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-211-01 : tests unitaires de {@link DivorceDcBeCalculator}.
 */
class DivorceDcBeCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_nominal_recevable() {
        DivorceDcBeResult r = DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 15),
                true, true, true, true,
                true, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("RECEVABLE");
        assertThat(r.delaiReflexionRespecte()).isTrue();
        assertThat(r.delaiReflexionJours()).isEqualTo(120L);
        assertThat(r.conventionComplete()).isTrue();
        assertThat(r.motifsIrrecevabilite()).isEmpty();
        assertThat(r.baseJuridique()).contains("1287").contains("229");
    }

    @Test
    void compute_conventionLogementManquante_irrecevable() {
        DivorceDcBeResult r = DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 15),
                false, true, true, true,
                false, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("IRRECEVABLE");
        assertThat(r.conventionComplete()).isFalse();
        assertThat(r.motifsIrrecevabilite()).anySatisfy(m -> assertThat(m).contains("logement"));
    }

    @Test
    void compute_conventionBiensManquante_irrecevable() {
        DivorceDcBeResult r = DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 15),
                true, false, true, true,
                false, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("IRRECEVABLE");
        assertThat(r.motifsIrrecevabilite()).anySatisfy(m -> assertThat(m).contains("biens"));
    }

    @Test
    void compute_conventionGardeEnfantsManquante_irrecevable() {
        DivorceDcBeResult r = DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 15),
                true, true, false, true,
                true, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("IRRECEVABLE");
        assertThat(r.motifsIrrecevabilite()).anySatisfy(m -> assertThat(m).contains("garde"));
    }

    @Test
    void compute_conventionContributionsManquante_irrecevable() {
        DivorceDcBeResult r = DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 15),
                true, true, true, false,
                false, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("IRRECEVABLE");
        assertThat(r.motifsIrrecevabilite()).anySatisfy(m -> assertThat(m).contains("contribution"));
    }

    @Test
    void compute_delaiReflexionInsuffisant_irrecevable() {
        // 30 jours seulement, minimum 90 → IRRECEVABLE
        DivorceDcBeResult r = DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                true, true, true, true,
                false, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.delaiReflexionJours()).isEqualTo(30L);
        assertThat(r.delaiReflexionRespecte()).isFalse();
        assertThat(r.verdict()).isEqualTo("IRRECEVABLE");
        assertThat(r.motifsIrrecevabilite()).anySatisfy(m -> assertThat(m).contains("réflexion"));
    }

    @Test
    void compute_sansConsentement_nonConcerne() {
        DivorceDcBeResult r = DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 15),
                true, true, true, true,
                false, false,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("NON_CONCERNE");
        assertThat(r.motifsIrrecevabilite()).anySatisfy(m -> assertThat(m).contains("DDI"));
    }

    @Test
    void compute_audienceNonPlanifiee_recevableSansVerifDelai() {
        // sans audience, le délai n'est pas vérifié → si convention complète + consentement → RECEVABLE
        DivorceDcBeResult r = DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 4, 1),
                null,
                true, true, true, true,
                false, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("RECEVABLE");
        assertThat(r.delaiReflexionJours()).isEqualTo(-1L);
        assertThat(r.delaiReflexionRespecte()).isFalse();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("non encore planifiée"));
    }

    @Test
    void compute_avecEnfantsMineurs_addsMessage() {
        DivorceDcBeResult r = DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 1, 15),
                LocalDate.of(2026, 5, 15),
                true, true, true, true,
                true, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.enfantsMineursCommuns()).isTrue();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("mineurs"));
    }

    @Test
    void compute_futureSignature_throws() {
        assertThatThrownBy(() -> DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 6, 1),
                null,
                true, true, true, true,
                false, true,
                clockAt(LocalDate.of(2026, 5, 11))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_audienceAvantSignature_throws() {
        assertThatThrownBy(() -> DivorceDcBeCalculator.compute(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 4, 1),
                true, true, true, true,
                false, true,
                clockAt(LocalDate.of(2026, 5, 11))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("antérieure");
    }

    @Test
    void compute_nullSignature_throws() {
        assertThatThrownBy(() -> DivorceDcBeCalculator.compute(
                null,
                null,
                true, true, true, true,
                false, true,
                clockAt(LocalDate.of(2026, 5, 11))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateSignatureConvention");
    }
}
