package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Résultat d'une recommandation de titre de séjour.
 */
public record TitleRecommendation(
        String code,
        String label,
        String country,
        String motif,
        String conditions,
        List<String> pieces,
        int delaiMoyenJours
) {}
