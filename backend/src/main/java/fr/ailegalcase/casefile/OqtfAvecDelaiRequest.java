package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record OqtfAvecDelaiRequest(
        LocalDate dateNotificationOqtf,
        String motifOqtf,
        Boolean recoursForme,
        LocalDate dateRecours
) {}
