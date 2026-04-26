package fr.ailegalcase.casefile;

/**
 * SF-FA-24-05 : requête d'analyse de validité d'une donation entre vifs (FR —
 * art. 893-958 + 902-906 + 920+ Cciv). Le pays n'est pas transmis dans le body
 * — il est dérivé de {@code caseFile.getWorkspace().getCountry()} côté service.
 */
public record DonationRequest(
        DonationCalculator.FormeDonation formeDonation,
        String dateDonation,
        Integer ageDonateurAns,
        Boolean saineDEsprit,
        Boolean capaciteDonateur,
        Boolean capaciteRecipiendaire,
        Boolean consentementLibre,
        Boolean objetDeterminé,
        Boolean respectFormalisme,
        Boolean respectQuotiteDisponible,
        // Notariée
        Boolean acteAuthentique,
        Boolean acceptationExpresse,
        // Manuelle
        Boolean remiseEffective,
        Boolean bienMeuble,
        // Indirect
        Boolean intentionLiberale,
        Boolean actePrincipalNeutre,
        // Déguisée
        Boolean apparenceOnerueuse,
        Boolean prixIncoherent,
        // Vices consentement
        Boolean vicesConsentementDol,
        Boolean erreurSubstantielle,
        // Révocation
        Boolean ingratitudeAvere,
        Boolean inexecutionCharge
) {}
