package fr.ailegalcase.jurisprudencemapping;

import jakarta.validation.constraints.Size;

/**
 * F-JU-01 / SF-JU-01-04 — payload du signalement utilisateur.
 *
 * @param comment commentaire libre optionnel ({@code null} ou ≤ 2000 caractères)
 */
public record JurisprudenceSignalRequest(
        @Size(max = 2000, message = "comment must be ≤ 2000 chars")
        String comment) {
}
