package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-180 SF-180-01 — Snapshot historisé d'un run d'audit dashboard.
 *
 * <p>Une row = un audit produit soit par le {@code @Scheduled} hebdomadaire
 * (lundi 8h UTC), soit par le bouton « Relancer maintenant » du super-admin.
 * Les 3 panels sont sérialisés en JSON {@code TEXT} (compat H2 + PostgreSQL —
 * pattern {@code prudhome_fiches}).</p>
 *
 * <p>L'endpoint {@code GET /dashboard-audit/latest} lit la dernière row par
 * {@code ran_at DESC} : il ne recalcule pas (un run = 95+ {@code count(*)}).</p>
 */
@Entity
@Table(name = "dashboard_audit_runs")
@Getter
@Setter
public class DashboardAuditRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ran_at", nullable = false)
    private Instant ranAt;

    /** JSON sérialisé de la liste des {@code CrashedMapper}. */
    @Column(name = "crashed_json", nullable = false, columnDefinition = "TEXT")
    private String crashedJson;

    /** JSON sérialisé de la liste des {@code TileTableCount} dormantes. */
    @Column(name = "dormant_json", nullable = false, columnDefinition = "TEXT")
    private String dormantJson;

    /** JSON sérialisé de la liste des {@code TileTableCount} actives. */
    @Column(name = "active_json", nullable = false, columnDefinition = "TEXT")
    private String activeJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
