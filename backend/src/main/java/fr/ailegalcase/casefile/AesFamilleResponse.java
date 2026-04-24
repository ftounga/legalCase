package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AesFamilleResponse(
        UUID caseFileId,
        LocalDate dateEntreeFrance,
        int dureePresenceMois,
        boolean conjointFrancaisOuRegulier,
        int enfantsScolarisesFrance,
        int dureeScolaritePlusAncienEnfantAnnees,
        boolean preuvesInsertion,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        String country,
        boolean presence5AnsOk,
        boolean presence10AnsOk,
        boolean liensFamiliauxOk,
        boolean insertionOk,
        boolean pasMenace,
        int scoreGlobal,
        String verdictProbabiliteAcceptation,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstructionSiDemande,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
