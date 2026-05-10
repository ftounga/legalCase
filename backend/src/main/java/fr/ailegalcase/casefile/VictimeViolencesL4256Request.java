package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record VictimeViolencesL4256Request(
        LocalDate dateOrdonnanceProtection,
        String juridiction,
        Integer dureeProtectionMois,
        LocalDate dateExpirationProtection,
        Integer enfantsAcharge,
        String nationalite
) {}
