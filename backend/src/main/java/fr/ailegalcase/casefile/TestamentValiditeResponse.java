package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-24-03 : réponse de l'endpoint {@code /testament-validite-analysis}.
 */
public record TestamentValiditeResponse(
        UUID caseFileId,
        TestamentValiditeCalculator.FormeTestament formeTestament,
        TestamentValiditeCalculator.VerdictValidite verdictValidite,
        List<TestamentValiditeCalculator.ViceIdentifie> vicesIdentifies,
        boolean actionEnReductionPossible,
        int delaiContestationAns,
        int scoreEligibilite,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
