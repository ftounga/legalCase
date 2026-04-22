package fr.ailegalcase.document.vision;

import java.util.UUID;

/**
 * SF-148-01 : événement publié après {@code DocumentPieceDetectionService.persistAll}
 * pour déclencher l'enrichissement visuel asynchrone (hors transaction).
 *
 * @param documentId    document cible
 * @param legalDomain   domaine du workspace ({@code DROIT_DU_TRAVAIL}, {@code DROIT_IMMIGRATION},
 *                      {@code DROIT_FAMILLE}) — peut être null si non résolu
 */
public record PiecesPersistedEvent(UUID documentId, String legalDomain) {}
