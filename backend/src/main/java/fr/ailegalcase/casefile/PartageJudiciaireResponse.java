package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-17-01 : réponse de l'endpoint {@code /partage-judiciaire-analysis}.
 */
public record PartageJudiciaireResponse(
        UUID caseFileId,
        PartageJudiciaireCalculator.VerdictRecevabilite verdictRecevabilite,
        int scoreEligibilite,
        int dureeProcedureMois,
        double fraisEstimesEur,
        boolean risqueLicitation,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
