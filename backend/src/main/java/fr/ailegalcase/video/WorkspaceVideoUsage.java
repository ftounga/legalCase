package fr.ailegalcase.video;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-231-03 — Trace ligne-par-ligne d'un upload vidéo consommant le quota
 * mensuel d'un workspace. Persistée par {@link VideoQuotaService} après
 * vérification du quota plan.
 *
 * <p>La requête principale {@code SUM(minutes_consumed) WHERE workspace_id = ?
 * AND year_month = ?} (cf. {@link WorkspaceVideoUsageRepository}) calcule la
 * consommation cumulée du mois courant — le quota plan est défini dans
 * {@link fr.ailegalcase.billing.PlanLimitService#getMonthlyVideoMinutes(String)}.
 *
 * <p>Cohérence : le pattern miroir des quotas OCR (F-122) avec un INSERT par
 * upload (vs compteur agrégé) permet un audit explicite par document et un
 * reset mensuel naturel via la colonne {@code year_month}.
 */
@Entity
@Table(name = "workspace_video_usage")
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceVideoUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    /** Format ISO `YYYY-MM` (ex: `2026-05`). Reset mensuel implicite via filtre `WHERE year_month = ?`. */
    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    /** Minutes consommées arrondies au-dessus : {@code Math.ceil(durationSeconds/60)}. */
    @Column(name = "minutes_consumed", nullable = false)
    private int minutesConsumed;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
