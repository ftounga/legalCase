package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-01 — audit log rejouable des actions sur les mappings
 * jurisprudentiels (auto-pilot Claude OU manuel SUPER_ADMIN).
 *
 * <p>{@code actorUserId} est renseigné uniquement quand {@code actor=SUPER_ADMIN}.
 * {@code claudeConfidence} et {@code claudeReason} sont renseignés uniquement
 * quand {@code actor=CRON}.</p>
 *
 * <p>Table créée en SF-JU-01-01 mais NON alimentée — aucune écriture avant
 * SF-JU-01-02 / SF-JU-01-05.</p>
 */
@Entity
@Table(name = "jurisprudence_audit_log")
@Getter
@Setter
public class JurisprudenceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mapping_id", nullable = false)
    private ToolJurisprudenceMapping mapping;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private JurisprudenceAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor", nullable = false, length = 20)
    private JurisprudenceAuditActor actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Column(name = "claude_confidence", precision = 3, scale = 2)
    private BigDecimal claudeConfidence;

    @Column(name = "claude_reason", length = 2000)
    private String claudeReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
