package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OqtfSansDelaiResponse(
        UUID caseFileId,
        LocalDateTime dateHeureNotificationOqtf,
        String motifSansDelai,
        boolean placementCra,
        boolean recoursForme,
        LocalDateTime dateHeureRecours,
        String country,
        LocalDateTime dateHeureExpirationDelaiRecours,
        long heuresRestantes,
        String statutDelaiRecours,
        LocalDateTime dateHeureAudiencePrevisionnelle,
        LocalDate dateDecisionPrevisionnelle,
        List<String> refereDisponibles,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
