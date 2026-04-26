package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-FA-18-09 : résultat structuré de l'analyse de recevabilité d'une
 * adoption (FR — art. 343-370-2 Cciv).
 */
public record AdoptionResult(
        AdoptionCalculator.FormeAdoption formeAdoption,
        int ageAdoptant,
        int ageAdopte,
        int differenceAgeAns,
        boolean consentementParents,
        boolean consentementAdopte,
        boolean consentementConjointAdoptant,
        boolean enquetes,
        boolean placement6mois,
        boolean pupilleEtat,
        boolean adoptantMarie,
        String country,
        AdoptionCalculator.VerdictRecevabilite verdictRecevabilite,
        AdoptionCalculator.FormeAdoption formeRecommandee,
        List<String> criteresNonRemplis,
        int delaiInstructionMois,
        List<String> documentsRequis,
        List<String> risqueRefus,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
