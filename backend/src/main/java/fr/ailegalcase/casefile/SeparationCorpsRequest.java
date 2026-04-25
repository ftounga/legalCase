package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record SeparationCorpsRequest(
        String modeProcedure,
        LocalDate dateJugementSeparationCorps,
        LocalDate dateRequeteConversion,
        Integer dureeSeparationAnnees,
        Boolean consentementMutuelConversion,
        Boolean patrimoineCommun,
        Integer enfantsMineurs,
        Boolean demandeReconciliationFormulee
) {}
