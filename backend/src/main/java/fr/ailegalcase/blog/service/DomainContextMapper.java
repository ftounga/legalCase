package fr.ailegalcase.blog.service;

import java.util.Map;

/**
 * Mapping catégorie de topic → fragment de contexte juridique injecté dans le
 * prompt Sonnet. Garantit la couverture des 3 domaines V1 (travail, immigration,
 * famille) sans branche conditionnelle dispersée.
 */
final class DomainContextMapper {

    private static final Map<String, String> CONTEXT_BY_CATEGORY = Map.of(
            "DROIT_DU_TRAVAIL",
            "Code du travail (FR) / Loi du 3 juillet 1978 et CCT (BE), prud'hommes (FR), " +
            "tribunal du travail (BE), conventions collectives, droit individuel et collectif",

            "DROIT_IMMIGRATION",
            "CESEDA (FR) / Loi du 15 décembre 1980 (BE), titres de séjour, autorisations de " +
            "travail, OQTF (FR) / OQT (BE), recours administratifs et juridictionnels",

            "DROIT_FAMILLE",
            "Code civil (FR + BE), divorce, autorité parentale, prestation compensatoire (FR) / " +
            "pension alimentaire (BE), procédures gracieuses et contentieuses"
    );

    private DomainContextMapper() {}

    static String contextFor(String category) {
        String context = CONTEXT_BY_CATEGORY.get(category);
        if (context == null) {
            throw new IllegalArgumentException("Unknown blog category: " + category);
        }
        return context;
    }
}
