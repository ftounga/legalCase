package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AesHumanitaireResponse(
        UUID caseFileId,
        LocalDate dateEntreeFrance,
        MotifHumanitaire motifHumanitaireDominant,
        boolean preuvesMedicales,
        boolean preuvesViolencesOuTraite,
        boolean demandeAsileDeposeeEtRejetee,
        boolean commissionTitreSejourSaisie,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        String country,
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
