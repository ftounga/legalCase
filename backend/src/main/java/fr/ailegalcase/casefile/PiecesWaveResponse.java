package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * F-283 / SF-283-02 — la « vague de pièces » en attente : les pièces ajoutées
 * depuis la dernière analyse dossier réussie. Rend lisible l'ajout incrémental
 * (« N nouvelles pièces depuis la dernière analyse ») au lieu d'une ré-analyse
 * opaque. Lecture seule : dérivé de {@code analysis_jobs} + {@code documents},
 * aucune table propre.
 *
 * @param analyzedAt    fin de la dernière analyse dossier réussie ({@code null} si aucune)
 * @param pendingCount  nombre de pièces ajoutées depuis (0 si pas d'analyse de référence)
 * @param pendingPieces les pièces concernées, les plus récentes d'abord
 */
public record PiecesWaveResponse(
        Instant analyzedAt,
        int pendingCount,
        List<PendingPiece> pendingPieces
) {
    public record PendingPiece(
            UUID documentId,
            String filename,
            Instant createdAt
    ) {}

    /** Dossier neuf / jamais analysé : pas de vague (l'onboarding initial est couvert ailleurs). */
    public static PiecesWaveResponse none() {
        return new PiecesWaveResponse(null, 0, List.of());
    }
}
