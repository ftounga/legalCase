package fr.ailegalcase.casefile;

import java.util.Map;

/**
 * SF-129-01 : normalise les codes convention collective retournés par l'IA en codes
 * exploitables par le référentiel (DB ou fallback static).
 *
 * L'IA peut retourner plusieurs formats :
 * - "IDCC_3043" (format canonique attendu)
 * - "3043" (numéro seul — on préfixe automatiquement)
 * - "IDCC 3043" (avec espace — on normalise)
 * - "METALLURGIE", "COMMERCE", "BTP", "HCR", "SYNTEC" (codes legacy — on mappe vers IDCC)
 * - "CP200", "CP124", "CP302" (Belgique — inchangés)
 * - "NETTOYAGE", "PROPRETE", etc. (codes descriptifs sans mapping IDCC garanti)
 */
public final class ConventionCodeNormalizer {

    /** Mapping des codes descriptifs legacy vers leur IDCC officiel. */
    private static final Map<String, String> LEGACY_ALIASES = Map.of(
            "METALLURGIE", "IDCC_3248",
            "COMMERCE",    "IDCC_2216",
            "BTP",         "IDCC_1596",
            "HCR",         "IDCC_1979",
            "SYNTEC",      "IDCC_1486"
    );

    private ConventionCodeNormalizer() {}

    /**
     * Normalise un code convention en un identifiant utilisable comme entry_key.
     * Retourne null si la valeur d'entrée est null/blank.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim().toUpperCase();
        if (trimmed.isEmpty()) return null;

        // Déjà au format IDCC_XXXX
        if (trimmed.matches("IDCC_\\d{4}")) return trimmed;

        // "IDCC 3043" ou "IDCC3043" → "IDCC_3043"
        if (trimmed.startsWith("IDCC")) {
            String digits = trimmed.substring(4).replaceAll("\\D", "");
            if (digits.length() >= 1 && digits.length() <= 4) {
                return "IDCC_" + padLeft(digits, 4);
            }
        }

        // Numéro pur : "3043" → "IDCC_3043"
        if (trimmed.matches("\\d{1,4}")) {
            return "IDCC_" + padLeft(trimmed, 4);
        }

        // Codes Belgique CP* inchangés
        if (trimmed.matches("CP\\d{2,4}")) return trimmed;

        // Mapping legacy
        String aliased = LEGACY_ALIASES.get(trimmed);
        if (aliased != null) return aliased;

        // Sinon retourne tel quel (ex. "NETTOYAGE", "PROPRETE") — le caller fallback
        // gèrera le cas où ce code n'est pas trouvé dans le référentiel.
        return trimmed;
    }

    private static String padLeft(String s, int length) {
        return "0".repeat(Math.max(0, length - s.length())) + s;
    }
}
