package fr.ailegalcase.jurisprudencemapping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * F-JU-01 / SF-JU-01-05 — payload bootstrap (1 à 200 entrées par batch).
 *
 * <p>SF-JU-06-03 : {@code enrichQueries} active l'enrichissement automatique des
 * requêtes JUDILIBRE ({@link JudilibreQueryEnricher}) avant l'appel de recherche.
 * Défaut {@code false} (comportement historique inchangé) — constructeur de
 * compatibilité {@code (entries)} conservé.</p>
 */
public record JurisprudenceBootstrapRequest(
        @NotEmpty
        @Size(min = 1, max = 200, message = "bootstrap accepts 1 to 200 entries per batch")
        @Valid
        List<JurisprudenceBootstrapEntry> entries,
        boolean enrichQueries) {

    /** Compatibilité : bootstrap sans enrichissement (comportement historique). */
    public JurisprudenceBootstrapRequest(List<JurisprudenceBootstrapEntry> entries) {
        this(entries, false);
    }
}
