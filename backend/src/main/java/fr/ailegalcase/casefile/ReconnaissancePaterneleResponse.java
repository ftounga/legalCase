package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-18-01 : réponse de l'endpoint
 * {@code /reconnaissance-paternelle-analysis}.
 */
public record ReconnaissancePaterneleResponse(
        UUID caseFileId,
        ReconnaissancePaterneleCalculator.SousType sousType,
        ReconnaissancePaterneleCalculator.VerdictRecevabilite verdictRecevabilite,
        int scoreEligibilite,
        LocalDate effetFiliation,
        List<String> risquesContestation,
        List<String> documentsRequis,
        int delaiContestationAns,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
