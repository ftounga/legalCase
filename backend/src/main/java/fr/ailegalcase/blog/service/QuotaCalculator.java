package fr.ailegalcase.blog.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Logique pure de calcul de déficit vs quota éditorial cible.
 * Pas de dépendance Spring — testable sans contexte.
 *
 * Quotas cibles :
 * - Catégories : 60 % travail / 25 % immigration / 15 % famille
 * - Pays : 70 % FR / 30 % BE
 *
 * Déficit relatif : cible − part observée. Plus la valeur est positive, plus la cible est prioritaire.
 */
class QuotaCalculator {

    static final Map<String, Double> TARGET_CATEGORY_SHARE = Map.of(
            "DROIT_DU_TRAVAIL", 0.60,
            "DROIT_IMMIGRATION", 0.25,
            "DROIT_FAMILLE", 0.15
    );

    static final Map<String, Double> TARGET_COUNTRY_SHARE = Map.of(
            "FR", 0.70,
            "BE", 0.30
    );

    /**
     * Retourne les catégories triées par déficit décroissant (la plus en retard d'abord).
     * Pour une catégorie absente de la map de comptes : part = 0, déficit = cible (prioritaire).
     */
    List<String> prioritizedCategories(Map<String, Long> countsByCategory) {
        long total = countsByCategory.values().stream().mapToLong(Long::longValue).sum();
        return TARGET_CATEGORY_SHARE.keySet().stream()
                .sorted(Comparator.comparingDouble(
                        (String cat) -> categoryDeficit(cat, countsByCategory, total)
                ).reversed())
                .toList();
    }

    /**
     * Pour une catégorie donnée, retourne les pays triés par déficit décroissant.
     * Utilise uniquement les articles de cette catégorie.
     */
    List<String> prioritizedCountriesForCategory(String category, Map<String, Long> countsByCountryInCategory) {
        long total = countsByCountryInCategory.values().stream().mapToLong(Long::longValue).sum();
        return TARGET_COUNTRY_SHARE.keySet().stream()
                .sorted(Comparator.comparingDouble(
                        (String co) -> countryDeficit(co, countsByCountryInCategory, total)
                ).reversed())
                .toList();
    }

    private double categoryDeficit(String category, Map<String, Long> counts, long total) {
        double target = TARGET_CATEGORY_SHARE.getOrDefault(category, 0.0);
        if (total == 0) {
            return target;
        }
        long count = counts.getOrDefault(category, 0L);
        double actual = (double) count / total;
        return target - actual;
    }

    private double countryDeficit(String country, Map<String, Long> counts, long total) {
        double target = TARGET_COUNTRY_SHARE.getOrDefault(country, 0.0);
        if (total == 0) {
            return target;
        }
        long count = counts.getOrDefault(country, 0L);
        double actual = (double) count / total;
        return target - actual;
    }
}
