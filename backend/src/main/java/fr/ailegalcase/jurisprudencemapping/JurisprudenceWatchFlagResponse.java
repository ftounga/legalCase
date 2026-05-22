package fr.ailegalcase.jurisprudencemapping;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-05 — représentation API d'un {@link JurisprudenceWatchFlag}.
 */
public record JurisprudenceWatchFlagResponse(
        UUID id,
        String toolId,
        String brancheCalculId,
        String arretEntrantRef,
        UUID mappingActuelId,
        JurisprudenceWatchFlagSource source,
        BigDecimal confidenceScore,
        String explication,
        JurisprudenceWatchFlagStatut statut,
        Instant createdAt,
        Instant reviewedAt,
        JurisprudenceWatchFlagDecision decision,
        String commentUser) {

    public static JurisprudenceWatchFlagResponse from(JurisprudenceWatchFlag flag) {
        return new JurisprudenceWatchFlagResponse(
                flag.getId(),
                flag.getToolId(),
                flag.getBrancheCalculId(),
                flag.getArretEntrantRef(),
                flag.getMappingActuel() != null ? flag.getMappingActuel().getId() : null,
                flag.getSource(),
                flag.getConfidenceScore(),
                flag.getExplication(),
                flag.getStatut(),
                flag.getCreatedAt(),
                flag.getReviewedAt(),
                flag.getDecision(),
                flag.getCommentUser());
    }
}
