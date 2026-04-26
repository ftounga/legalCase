package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AtMpResponse(
        UUID caseFileId,
        String country,
        String dispositif,
        String dispositifLibelle,
        LocalDate dateAccident,
        Boolean lieuTravail,
        Boolean declarationEmployeurDansLes48h,
        Boolean certificatMedicalInitial,
        String numeroTableau,
        Boolean delaiPriseEnChargeRespecte,
        LocalDate dateExposition,
        Integer tauxFixeParCpam,
        Integer tauxRevendique,
        Boolean expertiseMedicaleProduite,
        LocalDate datePremierAvisCpam,
        String verdictRecevabilite,
        int delaiInstructionJours,
        String competence,
        boolean expertiseRequise,
        List<String> documentsRequis,
        List<String> risqueRefus,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
