package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-18-07 : résultat structuré de l'analyse de recevabilité d'une
 * possession d'état (FR — art. 311-1 + 311-2 + 317 Cciv).
 */
public record PossessionEtatResult(
        LocalDate dateDebutPossession,
        LocalDate dateFinPossession,
        boolean tractatus,
        boolean fama,
        boolean nomen,
        boolean continueCondition,
        boolean paisible,
        boolean nonEquivoque,
        String country,
        PossessionEtatCalculator.VerdictRecevabilite verdictRecevabilite,
        PossessionEtatCalculator.DispositifApplicable dispositifApplicable,
        int scoreRecevabilite,
        int dureePossessionAnnees,
        int delaiContestationActeAns,
        int delaiContestationCessationAns,
        List<String> criteresRemplis,
        List<String> criteresManquants,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
