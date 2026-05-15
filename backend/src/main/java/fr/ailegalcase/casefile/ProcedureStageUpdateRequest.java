package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Size;

/**
 * F-243 / SF-243-01 — Corps de requête de l'endpoint C (mise à jour du stade procédural).
 *
 * <p>Les 3 champs sont <strong>nullable</strong> : envoyer {@code null} sur un champ l'efface.
 * La cohérence en cascade (effacer {@code jurisdiction} efface {@code stage} et
 * {@code position} ; effacer {@code stage} efface {@code position}) est appliquée côté service.
 *
 * <p>{@code @Size} garantit le format ; la validité sémantique (combinaison existante pour le
 * domaine du dossier) est vérifiée par {@link ProcedureStageService}.
 */
public record ProcedureStageUpdateRequest(
        @Size(max = 50, message = "jurisdiction : 50 caractères maximum") String jurisdiction,
        @Size(max = 50, message = "stage : 50 caractères maximum") String stage,
        @Size(max = 50, message = "position : 50 caractères maximum") String position
) {

    /** Normalise un code : trim + uppercase ; {@code null} ou chaîne vide → {@code null}. */
    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(java.util.Locale.ROOT);
    }

    /** @return le code juridiction normalisé (trim + uppercase, vide → null). */
    public String normalizedJurisdiction() {
        return normalize(jurisdiction);
    }

    /** @return le code stade normalisé (trim + uppercase, vide → null). */
    public String normalizedStage() {
        return normalize(stage);
    }

    /** @return le code position normalisé (trim + uppercase, vide → null). */
    public String normalizedPosition() {
        return normalize(position);
    }
}
