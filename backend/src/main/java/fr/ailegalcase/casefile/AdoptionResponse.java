package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-18-09 : réponse de l'endpoint
 * {@code /adoption-analysis}.
 */
public record AdoptionResponse(
        UUID caseFileId,
        AdoptionCalculator.FormeAdoption formeAdoption,
        AdoptionCalculator.FormeAdoption formeRecommandee,
        AdoptionCalculator.VerdictRecevabilite verdictRecevabilite,
        int ageAdoptant,
        int ageAdopte,
        int differenceAgeAns,
        List<String> criteresNonRemplis,
        int delaiInstructionMois,
        List<String> documentsRequis,
        List<String> risqueRefus,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
