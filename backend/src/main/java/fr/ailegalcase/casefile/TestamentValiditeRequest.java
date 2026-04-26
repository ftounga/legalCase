package fr.ailegalcase.casefile;

/**
 * SF-FA-24-03 : requête d'analyse de validité d'un testament (FR — art.
 * 967-1035 + 901-911 Cciv). Le pays n'est pas transmis dans le body — il
 * est dérivé de {@code caseFile.getWorkspace().getCountry()} côté service.
 */
public record TestamentValiditeRequest(
        TestamentValiditeCalculator.FormeTestament formeTestament,
        String dateRedaction,
        Integer ageTestateurAnsRedaction,
        Boolean saineDEsprit,
        Boolean majeurProtegeAvecAssistance,
        // Olographe
        Boolean ecritureManuscritIntegrale,
        Boolean dateComplete,
        Boolean signatureTestateur,
        // Authentique
        Boolean presenceNotaireEtTemoinsConforme,
        Boolean dicteEnPresence,
        Boolean lectureFinaleAuTestateur,
        Boolean signaturesCompletes,
        // Mystique
        Boolean remiseSousPliCache,
        Boolean declarationDevant2Temoins,
        Boolean acteSuscriptionNotaire,
        // International
        Boolean respecteFormeWashington,
        // Vices consentement
        Boolean vicesConsentementDol,
        Boolean erreurSubstantielle,
        // Révocation
        Boolean testamentPosterieurContradictoire,
        Boolean dechirureVolontaireOriginal,
        // Quotité disponible
        Boolean legsExcedeQuotiteDisponible
) {}
