package fr.ailegalcase.jurisprudencemapping;

import java.time.Instant;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-10 — réponse 202 Accepted du POST /api/admin/jurisprudence/bootstrap.
 *
 * <p>Le runner async {@code JurisprudenceBootstrapService.runBootstrapAsync} continue
 * en arrière-plan ; le frontend poll {@code GET /api/admin/jurisprudence/bootstrap/jobs/{id}}
 * pour suivre la progression.</p>
 */
public record JurisprudenceBootstrapJobStarted(
        UUID jobId,
        int entriesTotal,
        Instant startedAt) {
}
