package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Belgian9bisResponse(
        UUID caseFileId,
        LocalDate dateEntreeBelgique,
        int dureePresenceMois,
        boolean circonstancesExceptionnelles,
        boolean liensFamiliauxBe,
        boolean liensProfessionnels,
        boolean scolariteEnfantsBe,
        boolean menaceOrdrePublic,
        LocalDate dateDepotDemande,
        String country,
        boolean presence3AnsOk,
        boolean liensConstitutifsOk,
        boolean pasMenace,
        int scoreGlobal,
        String verdictProbabilite,
        List<String> criteresNonRemplis,
        LocalDate dateExpirationInstructionPrevisionnelle,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
