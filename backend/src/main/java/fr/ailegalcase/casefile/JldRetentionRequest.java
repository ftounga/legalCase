package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record JldRetentionRequest(
        LocalDate dateNotificationPlacement,
        String motifPlacement,
        Boolean recoursForme,
        LocalDate dateRecours
) {}
