package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

/**
 * Dérive une valeur de type_rupture à partir des signaux IA déjà extraits
 * (licenciement_validity_detection, qualification juridique / litige détecté)
 * lorsque l'IA n'a pas peuplé compensation_data.type_rupture.
 *
 * Garantit le pré-remplissage F-DT-09 et les alertes de cohérence F-IA-03
 * (SF-IA-03-05) même sur les dossiers où l'IA omet structurellement le champ.
 */
final class TypeRuptureFallback {

    static final Set<String> VALID_TYPES = Set.of(
            "LICENCIEMENT", "LICENCIEMENT_ECONOMIQUE", "RUPTURE_CONVENTIONNELLE",
            "DEMISSION", "PRISE_ACTE", "RESILIATION_JUDICIAIRE",
            "LICENCIEMENT_ORDINAIRE", "RUPTURE_AMIABLE"
    );

    /**
     * Valeurs obsolètes remontées par l'IA qu'on re-normalise vers le type de rupture réel.
     * "LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE" n'est pas une nature de rupture mais une
     * qualification juridique CCT 109 art. 8 (risque d'indemnité 3-17 sem.) — le vrai type
     * sous-jacent est toujours un licenciement ordinaire.
     * Le mapping assure le fail-soft sur les analyses déjà persistées avec l'ancien enum.
     */
    private static final java.util.Map<String, String> LEGACY_ALIASES = java.util.Map.of(
            "LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE", "LICENCIEMENT_ORDINAIRE"
    );

    private static final Set<String> FR_DETECTION_KEYS = Set.of(
            "FR_CONVOCATION", "FR_ENTRETIEN", "FR_DELAI_NOTIFICATION", "FR_MOTIVATION",
            "FR_MOTIF_REEL", "FR_PROCEDURE_DISCIPLINAIRE", "FR_ORDRE_LICENCIEMENT"
    );

    private static final Set<String> BE_DETECTION_KEYS = Set.of(
            "BE_NOTIFICATION", "BE_PREAVIS", "BE_MOTIVATION", "BE_AUDITION",
            "BE_NON_DISCRIMINATION", "BE_PROTECTION_SPECIALE", "BE_INDEMNITE_MANIFESTE"
    );

    private TypeRuptureFallback() {}

    /**
     * Essaie de dériver un type_rupture. Retourne null si aucun signal fiable.
     * Les valeurs retournées sont garanties upper-case et dans VALID_TYPES.
     */
    static String derive(JsonNode root) {
        if (root == null) return null;

        String fromLitige = deriveFromTypeLitige(root);
        if (fromLitige != null) return fromLitige;

        return deriveFromDetection(root);
    }

    private static String deriveFromTypeLitige(JsonNode root) {
        JsonNode node = root.get("type_litige_detecte");
        if (node == null || node.isNull() || !node.isTextual()) return null;
        String code = node.asText().trim().toUpperCase();
        return switch (code) {
            case "LICENCIEMENT_SANS_CAUSE_REELLE" -> "LICENCIEMENT";
            case "LICENCIEMENT_ECONOMIQUE" -> "LICENCIEMENT_ECONOMIQUE";
            case "PRISE_ACTE_RUPTURE" -> "PRISE_ACTE";
            default -> null;
        };
    }

    private static String deriveFromDetection(JsonNode root) {
        JsonNode node = root.get("licenciement_validity_detection");
        if (node == null || !node.isObject() || node.isEmpty()) return null;

        boolean hasFr = false;
        boolean hasBe = false;
        java.util.Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            String key = it.next().toUpperCase();
            JsonNode child = node.get(key);
            if (child == null || child.isNull()) continue;
            // On considère la clé active si une réponse existe, même "INCONNU"
            boolean active = child.isObject() && child.has("reponse") && !child.get("reponse").isNull()
                    && !child.get("reponse").asText("").isBlank();
            if (!active) continue;
            if (FR_DETECTION_KEYS.contains(key)) hasFr = true;
            else if (BE_DETECTION_KEYS.contains(key)) hasBe = true;
        }
        if (!hasFr && !hasBe) return null;
        // La présence d'une évaluation de validité de licenciement implique qu'un licenciement est analysé.
        return hasFr ? "LICENCIEMENT" : "LICENCIEMENT_ORDINAIRE";
    }

    /** Filtre les valeurs IA : upper-case + enum. Retourne null si invalide.
     *  Applique aussi un mapping de rétrocompatibilité pour les valeurs obsolètes
     *  qui pourraient rester dans des analyses déjà persistées. */
    static String normalize(String raw) {
        if (raw == null) return null;
        String up = raw.trim().toUpperCase();
        String aliased = LEGACY_ALIASES.get(up);
        if (aliased != null) return aliased;
        return VALID_TYPES.contains(up) ? up : null;
    }
}
