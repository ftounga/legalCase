package fr.ailegalcase.casefile.conclusion;

import java.time.Instant;
import java.util.UUID;

/**
 * F-98 / SF-98-52 — résumé d'une version de conclusions, élément de la liste
 * renvoyée par {@code GET .../conclusions/versions} (triée version décroissante).
 *
 * <p>Volontairement allégé : ne porte pas le {@code content} ni les libellés du
 * stade procédural — le détail complet d'une version passe par
 * {@code GET .../conclusions/versions/{versionId}} ({@link ConclusionResponse}).</p>
 */
public record ConclusionVersionSummary(
        UUID id,
        int versionNumber,
        String lifecycleStatus,
        String status,
        Instant generatedAt,
        Instant createdAt) {

    /** Construit le résumé à partir d'une version persistée. */
    public static ConclusionVersionSummary from(CaseConclusion conclusion) {
        return new ConclusionVersionSummary(
                conclusion.getId(),
                conclusion.getVersionNumber(),
                conclusion.getLifecycleStatus().name(),
                conclusion.getStatus().name(),
                conclusion.getGeneratedAt(),
                conclusion.getCreatedAt());
    }
}
