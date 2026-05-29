package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-214-15 : réponse de l'analyse récépissé vs attestation de prolongation
 * R. 311-4 / R. 311-6 CESEDA. Outil single-country FR.
 */
public record RecepisseAttestationResponse(
        UUID caseFileId,
        String typeDocument,
        String dateDelivrance,
        String dateExpiration,
        Boolean mentionAutorisationTravail,
        String country,
        boolean droitSejour,
        boolean droitTravail,
        Long dureeValiditeJours,
        boolean risqueEmployeur,
        List<String> recommandations,
        String baseJuridique
) {}
