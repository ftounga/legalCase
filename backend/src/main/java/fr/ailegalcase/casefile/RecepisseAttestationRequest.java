package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-15 : requête POST pour l'analyse récépissé vs attestation de
 * prolongation R. 311-4 / R. 311-6 CESEDA. Outil single-country FR.
 *
 * <p>{@code mentionAutorisationTravail} est optionnel (null si non renseigné).</p>
 */
public record RecepisseAttestationRequest(
        String typeDocument,
        LocalDate dateDelivrance,
        LocalDate dateExpiration,
        Boolean mentionAutorisationTravail
) {}
