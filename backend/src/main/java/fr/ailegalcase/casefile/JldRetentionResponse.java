package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record JldRetentionResponse(
        UUID caseFileId,
        LocalDate dateNotificationPlacement,
        String motifPlacement,
        boolean recoursForme,
        LocalDate dateRecours,
        String country,
        LocalDate dateExpirationSaisineJld,
        LocalDate dateAudienceJld,
        LocalDate dateExpirationRecoursAppel,
        long joursRestantsAvantSaisine,
        String statut,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
