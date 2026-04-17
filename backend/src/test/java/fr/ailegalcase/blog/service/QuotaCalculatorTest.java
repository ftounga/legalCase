package fr.ailegalcase.blog.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuotaCalculatorTest {

    private final QuotaCalculator calc = newInstance();

    @Test
    void prioritizedCategories_emptyCounts_returnsAllInTargetOrder() {
        List<String> result = calc.prioritizedCategories(Map.of());

        // Toutes les catégories ont un déficit = leur cible. Ordre : 60 % > 25 % > 15 %.
        assertThat(result).containsExactly(
                "DROIT_DU_TRAVAIL",
                "DROIT_IMMIGRATION",
                "DROIT_FAMILLE"
        );
    }

    @Test
    void prioritizedCategories_quotaRespected_returnsLowestRelativeShareFirst() {
        // 60 DT / 25 IM / 15 FA sur 100 → quotas respectés pile. Déficits = 0 pour tous.
        // Tie-breaker : ordre lexicographique indéfini, mais le premier doit être une catégorie neutre.
        Map<String, Long> counts = Map.of(
                "DROIT_DU_TRAVAIL", 60L,
                "DROIT_IMMIGRATION", 25L,
                "DROIT_FAMILLE", 15L
        );

        List<String> result = calc.prioritizedCategories(counts);

        assertThat(result).hasSize(3);
        assertThat(result).containsExactlyInAnyOrder(
                "DROIT_DU_TRAVAIL", "DROIT_IMMIGRATION", "DROIT_FAMILLE");
    }

    @Test
    void prioritizedCategories_travailUnderTarget_prioritizesTravail() {
        // Seulement 2 DT sur 10 (20 %) → déficit DT = 40 %. IM à 5/10 = 50 %, déficit = -25 %.
        Map<String, Long> counts = Map.of(
                "DROIT_DU_TRAVAIL", 2L,
                "DROIT_IMMIGRATION", 5L,
                "DROIT_FAMILLE", 3L
        );

        List<String> result = calc.prioritizedCategories(counts);

        assertThat(result.get(0)).isEqualTo("DROIT_DU_TRAVAIL");
    }

    @Test
    void prioritizedCategories_immigrationMostDeficit_returnsImmigrationFirst() {
        // 8 DT / 0 IM / 2 FA sur 10 → DT 80 % (déficit -20 %), IM 0 % (déficit 25 %), FA 20 % (déficit -5 %).
        Map<String, Long> counts = Map.of(
                "DROIT_DU_TRAVAIL", 8L,
                "DROIT_FAMILLE", 2L
        );

        List<String> result = calc.prioritizedCategories(counts);

        assertThat(result.get(0)).isEqualTo("DROIT_IMMIGRATION");
    }

    @Test
    void prioritizedCountriesForCategory_emptyCounts_returnsFRBeforeBE() {
        List<String> result = calc.prioritizedCountriesForCategory("DROIT_DU_TRAVAIL", Map.of());

        // Cible FR = 70 %, BE = 30 %. Déficit FR (70 %) > déficit BE (30 %).
        assertThat(result).containsExactly("FR", "BE");
    }

    @Test
    void prioritizedCountriesForCategory_onlyFR_prioritizesBE() {
        // 8 FR / 2 BE → FR 80 % (déficit -10 %), BE 20 % (déficit 10 %). BE prioritaire.
        Map<String, Long> counts = Map.of("FR", 8L, "BE", 2L);

        List<String> result = calc.prioritizedCountriesForCategory("DROIT_DU_TRAVAIL", counts);

        assertThat(result).containsExactly("BE", "FR");
    }

    @Test
    void prioritizedCountriesForCategory_ratio70_30_frSlightlyUnder_prioritizesFR() {
        // 6 FR / 4 BE → FR 60 % (déficit +10 %), BE 40 % (déficit -10 %). FR prioritaire.
        Map<String, Long> counts = Map.of("FR", 6L, "BE", 4L);

        List<String> result = calc.prioritizedCountriesForCategory("DROIT_DU_TRAVAIL", counts);

        assertThat(result).containsExactly("FR", "BE");
    }

    private static QuotaCalculator newInstance() {
        try {
            Constructor<QuotaCalculator> ctor = QuotaCalculator.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
