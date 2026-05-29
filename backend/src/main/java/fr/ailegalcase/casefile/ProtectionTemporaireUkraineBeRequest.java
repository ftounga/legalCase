package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-215-19 : body de la requête POST
 * {@code /api/v1/case-files/{id}/protection-temporaire-ukraine-be-analysis}.
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — protection temporaire Ukraine
 * (directive 2001/55/CE, décision UE 2022/382 du 04/03/2022).
 *
 * <p>{@code apatridesUkraine} et {@code membreFamilleProtege} sont optionnels
 * (défaut {@code false}) — ils élargissent le champ des bénéficiaires éligibles
 * au-delà de la nationalité ukrainienne stricte.
 */
public record ProtectionTemporaireUkraineBeRequest(
        @NotNull LocalDate dateArrivee,
        @NotNull Boolean nationaliteUkrainienne,
        @NotNull Boolean residenceUkraineAvant24Fev2022,
        Boolean apatridesUkraine,
        Boolean membreFamilleProtege,
        @NotNull ProtectionTemporaireUkraineBeTitreSejourEnum titreSejourBE
) {}
