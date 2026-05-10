package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CrrvRefusVisaResponse(
        UUID caseFileId,
        LocalDate dateNotificationRefus,
        String typeVisa,
        String motifRefus,
        boolean recoursForme,
        LocalDate dateRecours,
        String country,
        LocalDate dateExpirationRecoursCrrv,
        long joursRestants,
        String statut,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
