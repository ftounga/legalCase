package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OqtfAvecDelaiResponse(
        UUID caseFileId,
        LocalDate dateNotificationOqtf,
        String motifOqtf,
        boolean recoursForme,
        LocalDate dateRecours,
        String country,
        LocalDate dateExpirationDdv,
        LocalDate dateExpirationDelaiRecours,
        int joursRestantsAvantExpirationDelai,
        String statutDelaiRecours,
        LocalDate dateAudiencePrevisionnelle,
        LocalDate dateDecisionTaPrevisionnelle,
        List<String> referedDisponibles,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
