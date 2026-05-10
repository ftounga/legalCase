package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VictimeViolencesL4256Response(
        UUID caseFileId,
        LocalDate dateOrdonnanceProtection,
        String juridiction,
        int dureeProtectionMois,
        LocalDate dateExpirationProtectionEffective,
        int enfantsAcharge,
        String nationalite,
        String country,
        String eligibiliteScore,
        List<String> criteresValides,
        List<String> criteresManquants,
        int dureeTitreSejourMois,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
