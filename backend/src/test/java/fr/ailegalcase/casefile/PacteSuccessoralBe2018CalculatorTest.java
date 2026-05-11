package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-211-04 : tests unitaires de {@link PacteSuccessoralBe2018Calculator}.
 */
class PacteSuccessoralBe2018CalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_nominal_valide() {
        PacteSuccessoralBe2018Result r = PacteSuccessoralBe2018Calculator.compute(
                LocalDate.of(2025, 4, 15),
                true, true, true, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("VALIDE");
        assertThat(r.motifsAnnulation()).isEmpty();
        assertThat(r.motifsContestabilite()).isEmpty();
        assertThat(r.baseJuridique()).contains("1100/1");
    }

    @Test
    void compute_acteSousSeingPrive_nul() {
        PacteSuccessoralBe2018Result r = PacteSuccessoralBe2018Calculator.compute(
                LocalDate.of(2025, 4, 15),
                false, true, true, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("NUL");
        assertThat(r.motifsAnnulation()).anySatisfy(m -> assertThat(m).contains("authentique"));
    }

    @Test
    void compute_heritiersNonAppelles_nul() {
        PacteSuccessoralBe2018Result r = PacteSuccessoralBe2018Calculator.compute(
                LocalDate.of(2025, 4, 15),
                true, true, true, false,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("NUL");
        assertThat(r.motifsAnnulation()).anySatisfy(m -> assertThat(m).contains("appelés"));
    }

    @Test
    void compute_pasAccordTousHeritiers_contestable() {
        // tous présents, mais pas d'accord → CONTESTABLE
        PacteSuccessoralBe2018Result r = PacteSuccessoralBe2018Calculator.compute(
                LocalDate.of(2025, 4, 15),
                true, false, true, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("CONTESTABLE");
        assertThat(r.motifsContestabilite()).anySatisfy(m -> assertThat(m).contains("accord"));
        assertThat(r.motifsAnnulation()).isEmpty();
    }

    @Test
    void compute_donationsDesequilibrees_contestable() {
        PacteSuccessoralBe2018Result r = PacteSuccessoralBe2018Calculator.compute(
                LocalDate.of(2025, 4, 15),
                true, true, false, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("CONTESTABLE");
        assertThat(r.motifsContestabilite()).anySatisfy(m -> assertThat(m).contains("Déséquilibre"));
        assertThat(r.motifsAnnulation()).isEmpty();
    }

    @Test
    void compute_nulPrimeContestable() {
        // acte sous seing privé + déséquilibre → NUL prime sur CONTESTABLE
        PacteSuccessoralBe2018Result r = PacteSuccessoralBe2018Calculator.compute(
                LocalDate.of(2025, 4, 15),
                false, true, false, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("NUL");
        assertThat(r.motifsAnnulation()).isNotEmpty();
        assertThat(r.motifsContestabilite()).isNotEmpty();
    }

    @Test
    void compute_datePre2018_throws() {
        assertThatThrownBy(() -> PacteSuccessoralBe2018Calculator.compute(
                LocalDate.of(2018, 8, 31),
                true, true, true, true,
                clockAt(LocalDate.of(2026, 5, 11))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2018");
    }

    @Test
    void compute_dateExacteEntreeEnVigueur_valide() {
        PacteSuccessoralBe2018Result r = PacteSuccessoralBe2018Calculator.compute(
                LocalDate.of(2018, 9, 1),
                true, true, true, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.verdict()).isEqualTo("VALIDE");
    }

    @Test
    void compute_futureSignature_throws() {
        assertThatThrownBy(() -> PacteSuccessoralBe2018Calculator.compute(
                LocalDate.of(2026, 6, 1),
                true, true, true, true,
                clockAt(LocalDate.of(2026, 5, 11))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_nullDate_throws() {
        assertThatThrownBy(() -> PacteSuccessoralBe2018Calculator.compute(
                null,
                true, true, true, true,
                clockAt(LocalDate.of(2026, 5, 11))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateSignaturePacte");
    }

    @Test
    void compute_messages_inclusFormalitesObligatoires() {
        PacteSuccessoralBe2018Result r = PacteSuccessoralBe2018Calculator.compute(
                LocalDate.of(2025, 4, 15),
                true, true, true, true,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("authentique"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("réservataires"));
    }
}
