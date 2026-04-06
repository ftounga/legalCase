package fr.ailegalcase.casefile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record TribunalTravailFicheRequest(
        @Valid Requerant requerant,
        @Valid Defendeur defendeur,
        @Valid ProcedureInfo procedureInfo,
        @Valid ContratInfo contratInfo,
        List<@Valid Demande> demandes,
        String exposeDesMoyens
) {
    public record Requerant(
            @NotBlank String nom,
            String prenom,
            String domicile,
            String registreNational
    ) {}

    public record Defendeur(
            String nom,
            String siegeSocial,
            String numeroBce,
            String representant
    ) {}

    public record ProcedureInfo(
            String tribunal,
            String division,
            String langue,
            String commissionParitaire
    ) {}

    public record ContratInfo(
            String typeContrat,
            String dateDebut,
            String dateFin,
            String motifRupture
    ) {}

    public record Demande(
            @NotBlank String label,
            @PositiveOrZero Double montant
    ) {}
}
