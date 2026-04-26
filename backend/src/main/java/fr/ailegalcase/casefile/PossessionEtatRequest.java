package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-FA-18-07 : requête d'analyse de recevabilité d'une possession d'état
 * (FR — art. 311-1 + 311-2 + 317 Cciv).
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record PossessionEtatRequest(
        LocalDate dateDebutPossession,
        LocalDate dateFinPossession,
        Boolean tractatus,
        Boolean fama,
        Boolean nomen,
        Boolean continueCondition,
        Boolean paisible,
        Boolean nonEquivoque
) {}
