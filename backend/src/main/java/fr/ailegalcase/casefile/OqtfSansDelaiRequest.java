package fr.ailegalcase.casefile;

import java.time.LocalDateTime;

public record OqtfSansDelaiRequest(
        LocalDateTime dateHeureNotificationOqtf,
        String motifSansDelai,
        Boolean placementCra,
        Boolean recoursForme,
        LocalDateTime dateHeureRecours
) {}
