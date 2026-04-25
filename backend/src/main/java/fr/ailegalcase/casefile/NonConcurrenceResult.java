package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-24-01 : résultat structuré de l'analyse de validité d'une clause de
 * non-concurrence (FR — Cass. soc. 10/07/2002 + L.1221-1).
 */
public record NonConcurrenceResult(
        boolean critere1TerritoireOk,
        boolean critere2DureeOk,
        boolean critere3ObjetOk,
        boolean critere4ContrepartieOk,
        BigDecimal ratioContrepartiePct,
        int scoreValidite,
        NonConcurrenceCalculator.VerdictValidite verdictValidite,
        BigDecimal indemniteContrepartieDueEur,
        BigDecimal indemnitePotentielleNulliteEur,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
