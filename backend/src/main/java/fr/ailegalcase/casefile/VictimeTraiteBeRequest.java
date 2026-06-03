package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-221-06 : body de la requête POST
 * {@code /api/v1/case-files/{id}/victime-traite-be-analysis}.
 *
 * <p>Titre de séjour victime de la traite des êtres humains (BE — art. 61/2 et s. Loi
 * 15/12/1980 ; circulaire du 26/09/2008). {@code phaseProcedure} est requise et validée
 * contre la whitelist {@link VictimeTraiteBePhase} (sinon 400).
 */
public record VictimeTraiteBeRequest(
        @NotNull VictimeTraiteBePhase phaseProcedure,
        Boolean ruptureAvecReseau,
        Boolean cooperationJudiciaire,
        Boolean accompagnementCentreSpecialise,
        LocalDate dateDebutAccompagnement
) {}
