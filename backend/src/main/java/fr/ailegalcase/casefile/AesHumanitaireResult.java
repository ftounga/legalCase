package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-09-03 : résultat de l'analyse AES voie humanitaire (art. L.435-2
 * CESEDA + L.432-14 commission titre de séjour).
 * Outil single-country FR — pas d'équivalent BE standard.
 */
public record AesHumanitaireResult(
        LocalDate dateEntreeFrance,
        MotifHumanitaire motifHumanitaireDominant,
        boolean preuvesMedicales,
        boolean preuvesViolencesOuTraite,
        boolean demandeAsileDeposeeEtRejetee,
        boolean commissionTitreSejourSaisie,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        boolean motifEligible,
        boolean preuvesAdaptees,
        boolean commissionRequise,
        boolean pasMenace,
        int scoreGlobal,
        String verdictProbabiliteAcceptation,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstruction,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
