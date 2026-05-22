package fr.ailegalcase.jurisprudencemapping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * F-JU-01 / SF-JU-01-05 — entrée bootstrap : un mapping potentiel
 * (outil × branche) à initialiser via Claude + JUDILIBRE.
 */
public record JurisprudenceBootstrapEntry(

        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9_-]{1,100}$")
        String toolId,

        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9_-]{1,100}$")
        String brancheCalculId,

        @NotBlank
        @Size(max = 500)
        String motCleRecherche,

        @Size(max = 50)
        String juridictionFiltre,

        LocalDate dateMin) {
}
