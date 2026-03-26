package fr.ailegalcase.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank(message = "Le nom du workspace est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String name,

        @NotBlank(message = "Le domaine juridique est obligatoire")
        @Pattern(regexp = "DROIT_DU_TRAVAIL|DROIT_IMMIGRATION|DROIT_FAMILLE",
                message = "Domaine juridique non reconnu")
        @Size(max = 50)
        String legalDomain,

        @NotBlank(message = "Le pays est obligatoire")
        @Pattern(regexp = "FRANCE|BELGIQUE",
                message = "Pays non reconnu — valeurs acceptées : FRANCE, BELGIQUE")
        String country
) {}
