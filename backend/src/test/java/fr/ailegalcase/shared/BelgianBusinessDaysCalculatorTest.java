package fr.ailegalcase.shared;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BelgianBusinessDaysCalculatorTest {

    @Test
    void countBusinessDays_mondayToTuesday_is1() {
        // Lundi 2/3/2026 → Mardi 3/3/2026 : from exclu, to inclus = 1 jour ouvrable
        int n = BelgianBusinessDaysCalculator.countBusinessDays(
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 3));
        assertThat(n).isEqualTo(1);
    }

    @Test
    void countBusinessDays_fridayToMonday_is1() {
        // Vendredi 6/3/2026 → Lundi 9/3/2026 : samedi + dimanche exclus, lundi compté = 1
        int n = BelgianBusinessDaysCalculator.countBusinessDays(
                LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 9));
        assertThat(n).isEqualTo(1);
    }

    @Test
    void countBusinessDays_includesSaturdaySundayExclusion() {
        // Jeudi 5/3/2026 → Mardi 10/3/2026 : ven + lun + mar = 3 jours (sa/di exclus)
        int n = BelgianBusinessDaysCalculator.countBusinessDays(
                LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 10));
        assertThat(n).isEqualTo(3);
    }

    @Test
    void countBusinessDays_publicHolidayExcluded_1Mai2026() {
        // 1er mai 2026 = vendredi (férié en BE)
        // Jeudi 30/4/2026 → Lundi 4/5/2026 : 1/5 (férié) + sa + di exclus, seul 4/5 compte = 1
        int n = BelgianBusinessDaysCalculator.countBusinessDays(
                LocalDate.of(2026, 4, 30), LocalDate.of(2026, 5, 4));
        assertThat(n).isEqualTo(1);
    }

    @Test
    void countBusinessDays_sameDay_is0() {
        int n = BelgianBusinessDaysCalculator.countBusinessDays(
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2));
        assertThat(n).isZero();
    }

    @Test
    void countBusinessDays_toBeforeFrom_throws() {
        assertThatThrownBy(() -> BelgianBusinessDaysCalculator.countBusinessDays(
                LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countBusinessDays_nullFrom_throws() {
        assertThatThrownBy(() -> BelgianBusinessDaysCalculator.countBusinessDays(
                null, LocalDate.of(2026, 3, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isPublicHoliday_1janvier() {
        assertThat(BelgianBusinessDaysCalculator.isPublicHoliday(
                LocalDate.of(2026, 1, 1))).isTrue();
    }

    @Test
    void isPublicHoliday_21juillet() {
        assertThat(BelgianBusinessDaysCalculator.isPublicHoliday(
                LocalDate.of(2026, 7, 21))).isTrue();
    }

    @Test
    void isPublicHoliday_25decembre() {
        assertThat(BelgianBusinessDaysCalculator.isPublicHoliday(
                LocalDate.of(2026, 12, 25))).isTrue();
    }

    @Test
    void isPublicHoliday_regularDay_returnsFalse() {
        // Lundi 16/3/2026 = jour ouvrable normal
        assertThat(BelgianBusinessDaysCalculator.isPublicHoliday(
                LocalDate.of(2026, 3, 16))).isFalse();
    }

    @Test
    void computeEasterDate_2026_isApril5() {
        // Pâques 2026 = dimanche 5 avril
        LocalDate easter = BelgianBusinessDaysCalculator.computeEasterDate(2026);
        assertThat(easter).isEqualTo(LocalDate.of(2026, 4, 5));
    }

    @Test
    void computeEasterDate_2024_isMarch31() {
        // Pâques 2024 = dimanche 31 mars
        LocalDate easter = BelgianBusinessDaysCalculator.computeEasterDate(2024);
        assertThat(easter).isEqualTo(LocalDate.of(2024, 3, 31));
    }

    @Test
    void computeEasterDate_2025_isApril20() {
        // Pâques 2025 = dimanche 20 avril
        LocalDate easter = BelgianBusinessDaysCalculator.computeEasterDate(2025);
        assertThat(easter).isEqualTo(LocalDate.of(2025, 4, 20));
    }

    @Test
    void holidaysForYear_2026_contains10Holidays() {
        Set<LocalDate> holidays = BelgianBusinessDaysCalculator.holidaysForYear(2026);
        assertThat(holidays).hasSize(10);
        assertThat(holidays).contains(
                LocalDate.of(2026, 1, 1),   // Jour de l'An
                LocalDate.of(2026, 4, 6),   // Lundi de Pâques (Pâques 5/4 + 1)
                LocalDate.of(2026, 5, 1),   // Fête du Travail
                LocalDate.of(2026, 5, 14),  // Ascension (Pâques + 39 = 5/4 + 39 jours)
                LocalDate.of(2026, 5, 25),  // Lundi de Pentecôte (Pâques + 50)
                LocalDate.of(2026, 7, 21),  // Fête nationale
                LocalDate.of(2026, 8, 15),  // Assomption
                LocalDate.of(2026, 11, 1),  // Toussaint
                LocalDate.of(2026, 11, 11), // Armistice
                LocalDate.of(2026, 12, 25)  // Noël
        );
    }

    @Test
    void isBusinessDay_saturday_false() {
        // Samedi 7/3/2026
        assertThat(BelgianBusinessDaysCalculator.isBusinessDay(LocalDate.of(2026, 3, 7))).isFalse();
    }

    @Test
    void isBusinessDay_sunday_false() {
        // Dimanche 8/3/2026
        assertThat(BelgianBusinessDaysCalculator.isBusinessDay(LocalDate.of(2026, 3, 8))).isFalse();
    }

    @Test
    void isBusinessDay_holidayFriday_false() {
        // 1er mai 2026 = vendredi férié
        assertThat(BelgianBusinessDaysCalculator.isBusinessDay(LocalDate.of(2026, 5, 1))).isFalse();
    }

    @Test
    void isBusinessDay_regularMonday_true() {
        assertThat(BelgianBusinessDaysCalculator.isBusinessDay(LocalDate.of(2026, 3, 2))).isTrue();
    }
}
