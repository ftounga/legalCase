package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-24-09 : réponse de l'endpoint {@code /partage-successoral-analysis}.
 */
public record PartageSuccessoralResponse(
        UUID caseFileId,
        PartageSuccessoralCalculator.VerdictRecevabilite verdictRecevabilite,
        PartageSuccessoralCalculator.ModePartage modeRecommande,
        boolean basculeMode,
        int scoreEligibilite,
        int delaiInstructionMois,
        double fraisEstimesPct,
        double fraisEstimesEur,
        boolean risqueLicitation,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
