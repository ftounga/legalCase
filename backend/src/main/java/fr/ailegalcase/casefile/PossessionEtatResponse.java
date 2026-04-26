package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-18-07 : réponse de l'endpoint
 * {@code /possession-etat-analysis}.
 */
public record PossessionEtatResponse(
        UUID caseFileId,
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
        List<String> messages,
        String country
) {}
