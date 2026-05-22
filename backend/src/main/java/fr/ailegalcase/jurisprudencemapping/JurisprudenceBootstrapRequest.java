package fr.ailegalcase.jurisprudencemapping;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * F-JU-01 / SF-JU-01-05 — payload bootstrap (1 à 200 entrées par batch).
 */
public record JurisprudenceBootstrapRequest(
        @NotEmpty
        @Size(min = 1, max = 200, message = "bootstrap accepts 1 to 200 entries per batch")
        @Valid
        List<JurisprudenceBootstrapEntry> entries) {
}
