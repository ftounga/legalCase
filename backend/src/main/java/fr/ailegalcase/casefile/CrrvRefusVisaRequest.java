package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record CrrvRefusVisaRequest(
        LocalDate dateNotificationRefus,
        String typeVisa,
        String motifRefus,
        Boolean recoursForme,
        LocalDate dateRecours
) {}
