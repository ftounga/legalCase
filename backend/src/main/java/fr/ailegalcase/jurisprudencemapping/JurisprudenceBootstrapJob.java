package fr.ailegalcase.jurisprudencemapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-10 — job de bootstrap manuel du super-admin lancé en
 * {@code @Async} pour ne pas bloquer le client HTTP (timeout NGINX 120s).
 *
 * <p>Cycle de vie : RUNNING (au démarrage) → DONE (terminé OK) ou FAILED
 * (exception fatale dans le runner — {@code errorMessage} rempli).</p>
 *
 * <p>Mis à jour après chaque entrée traitée — visible immédiatement par le
 * frontend qui poll {@code GET /api/admin/jurisprudence/bootstrap/jobs/{id}}
 * toutes les 5 secondes.</p>
 */
@Entity
@Table(name = "jurisprudence_bootstrap_jobs")
@Getter
@Setter
public class JurisprudenceBootstrapJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private JurisprudenceBootstrapJobStatus status;

    @Column(name = "entries_total", nullable = false)
    private int entriesTotal;

    @Column(name = "entries_processed", nullable = false)
    private int entriesProcessed;

    @Column(name = "mappings_created", nullable = false)
    private int mappingsCreated;

    @Column(name = "entries_skipped", nullable = false)
    private int entriesSkipped;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "triggered_by_user_id")
    private UUID triggeredByUserId;

    @PrePersist
    void onPrePersist() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = JurisprudenceBootstrapJobStatus.RUNNING;
        }
    }
}
