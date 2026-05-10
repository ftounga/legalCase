package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DublinRecoursResponse(
        UUID caseFileId,
        LocalDate dateNotificationDecisionTransfert,
        String etatMembreResponsable,
        String motifTransfert,
        boolean recoursForme,
        LocalDate dateRecours,
        String country,
        LocalDate dateExpirationRecours,
        LocalDate dateLimiteTransfertEffectif,
        long joursRestants,
        String statut,
        String effetSuspensif,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
