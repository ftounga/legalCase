package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-221-01 : body de la requête POST
 * {@code /api/v1/case-files/{id}/carte-a-prorogation-be-analysis}.
 *
 * <p>{@code dateDemande} est nullable : requise uniquement lorsque
 * {@code demandeDeposee=true} (validation portée par le calculator → 400).
 */
public record CarteAProrogationBeRequest(
        @NotNull LocalDate dateExpirationCarteA,
        @NotNull Boolean motifSejourPersiste,
        @NotNull Boolean conditionsInitialesToujoursReunies,
        @NotNull Boolean demandeDeposee,
        LocalDate dateDemande
) {}
