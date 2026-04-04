package fr.ailegalcase.casefile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record PrudhomeFicheRequest(
        @Valid Demandeur demandeur,
        @Valid Defendeur defendeur,
        List<@Valid Demande> demandes,
        String faitsTexte,
        String moyensDroitTexte
) {
    public record Demandeur(
            @NotBlank String nom,
            String prenom,
            String adresse,
            String telephone,
            String email,
            String profession
    ) {}

    public record Defendeur(
            String nom,
            String adresse,
            String siret,
            String representant
    ) {}

    public record Demande(
            @NotBlank String label,
            @PositiveOrZero Double montant
    ) {}
}
