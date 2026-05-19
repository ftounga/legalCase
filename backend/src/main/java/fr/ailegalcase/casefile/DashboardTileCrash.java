package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-180 SF-180-01 — Crash runtime d'un mapper {@link DashboardTile} de F-167.
 *
 * <p>Une row = une exception réelle jetée en production par un mapper
 * {@code tileFromXxx()} et catchée par {@code CaseFileDashboardService.addSafely()}.
 * Persistée en DB plutôt que loggée seulement : robuste au redémarrage JVM et
 * historisée 30 jours (purge par {@code DashboardAuditService.runAudit()}).</p>
 *
 * <p>Complément <strong>runtime</strong> du garde-fou <strong>statique</strong>
 * {@code DashboardTileToolIdIntegrityIT} (SF-DT-36-03, CI build-time).</p>
 *
 * <p>Pas de {@code workspace_id} : observabilité produit globale (pattern des
 * tables {@code backlog_*} de F-178). {@code caseFileId} stocké pour usage
 * interne futur — jamais exposé par l'API (PII).</p>
 */
@Entity
@Table(name = "dashboard_tile_crashes")
@Getter
@Setter
public class DashboardTileCrash {

    /** Longueur max du message d'exception persisté — tronqué au-delà. */
    public static final int MAX_MESSAGE_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tool_id", nullable = false, length = 100)
    private String toolId;

    /** Nullable — un crash peut survenir hors contexte caseFile résolu. */
    @Column(name = "case_file_id")
    private UUID caseFileId;

    @Column(name = "exception_class", nullable = false, length = 255)
    private String exceptionClass;

    @Column(name = "exception_message", length = MAX_MESSAGE_LENGTH)
    private String exceptionMessage;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void onPrePersist() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
