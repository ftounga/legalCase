package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-FA-24-05 : résultat structuré du calcul de validité d'une donation entre
 * vifs (FR — art. 893-958 + 902-906 + 920+ Cciv).
 */
public record DonationResult(
        DonationCalculator.FormeDonation formeDonation,
        String dateDonation,
        int ageDonateurAns,
        Boolean saineDEsprit,
        Boolean capaciteDonateur,
        Boolean capaciteRecipiendaire,
        Boolean consentementLibre,
        Boolean objetDeterminé,
        Boolean respectFormalisme,
        Boolean respectQuotiteDisponible,
        Boolean acteAuthentique,
        Boolean acceptationExpresse,
        Boolean remiseEffective,
        Boolean bienMeuble,
        Boolean intentionLiberale,
        Boolean actePrincipalNeutre,
        Boolean apparenceOnerueuse,
        Boolean prixIncoherent,
        Boolean vicesConsentementDol,
        Boolean erreurSubstantielle,
        Boolean ingratitudeAvere,
        Boolean inexecutionCharge,
        String country,
        DonationCalculator.VerdictValidite verdictValidite,
        List<DonationCalculator.RisqueIdentifie> risquesRequalification,
        boolean actionEnReductionPossible,
        boolean revocationPossible,
        int delaiContestationAns,
        int scoreEligibilite,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
