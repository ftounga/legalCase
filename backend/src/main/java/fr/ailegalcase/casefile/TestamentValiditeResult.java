package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-FA-24-03 : résultat structuré du calcul de validité d'un testament
 * (FR — art. 967-1035 + 901-911 Cciv).
 */
public record TestamentValiditeResult(
        TestamentValiditeCalculator.FormeTestament formeTestament,
        String dateRedaction,
        int ageTestateurAnsRedaction,
        Boolean saineDEsprit,
        Boolean majeurProtegeAvecAssistance,
        Boolean ecritureManuscritIntegrale,
        Boolean dateComplete,
        Boolean signatureTestateur,
        Boolean presenceNotaireEtTemoinsConforme,
        Boolean dicteEnPresence,
        Boolean lectureFinaleAuTestateur,
        Boolean signaturesCompletes,
        Boolean remiseSousPliCache,
        Boolean declarationDevant2Temoins,
        Boolean acteSuscriptionNotaire,
        Boolean respecteFormeWashington,
        Boolean vicesConsentementDol,
        Boolean erreurSubstantielle,
        Boolean testamentPosterieurContradictoire,
        Boolean dechirureVolontaireOriginal,
        Boolean legsExcedeQuotiteDisponible,
        String country,
        TestamentValiditeCalculator.VerdictValidite verdictValidite,
        List<TestamentValiditeCalculator.ViceIdentifie> vicesIdentifies,
        boolean actionEnReductionPossible,
        int delaiContestationAns,
        int scoreEligibilite,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
