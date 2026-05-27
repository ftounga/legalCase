package fr.ailegalcase.jurisprudencemapping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * F-JU-01 / SF-JU-01-15 — payload de création manuelle d'un mapping
 * jurisprudentiel par un SUPER_ADMIN, quand le bootstrap auto-pilot Claude
 * n'a pas trouvé de candidat satisfaisant (ex. mots-clés trop génériques,
 * branches BE non couvertes par JUDILIBRE FR).
 *
 * <p>Tous les champs sont obligatoires — la saisie manuelle implique que
 * l'admin a déjà identifié l'arrêt et possède toutes ses métadonnées.</p>
 */
public record ManualMappingCreateRequest(

        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9_-]{1,100}$")
        String toolId,

        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9_-]{1,100}$")
        String brancheCalculId,

        @NotBlank
        @Size(max = 200)
        String arretRef,

        @NotBlank
        @Size(max = 50)
        String juridiction,

        @NotNull
        LocalDate dateArret,

        @NotBlank
        @Size(max = 50)
        String numeroPourvoi,

        @NotBlank
        @Size(max = 500)
        String lienLegifrance,

        @NotBlank
        @Size(max = 2000)
        String chapeauOfficiel) {
}
