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
 * F-JU-01 / SF-JU-01-01 — flag de veille jurisprudentielle à arbitrer.
 *
 * <p>Source {@code CRON} : nouveau arrêt potentiellement impactant détecté par le
 * cron mensuel (SF-JU-01-02). Source {@code USER_SIGNAL} : signalement d'un
 * avocat via le bouton « Signaler un problème » (SF-JU-01-04).</p>
 *
 * <p>Le statut passe de {@code PENDING} à {@code REVIEWED} (avec décision) ou
 * {@code IGNORED} lors du clic admin dans le dashboard
 * {@code /super-admin/jurisprudence-watch} (SF-JU-01-05).</p>
 *
 * <p>Table créée en SF-JU-01-01 mais NON alimentée — aucune écriture avant
 * SF-JU-01-02 / SF-JU-01-04.</p>
 */
@Entity
@Table(name = "jurisprudence_watch_flags")
@Getter
@Setter
public class JurisprudenceWatchFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tool_id", nullable = false, length = 100)
    private String toolId;

    @Column(name = "branche_calcul_id", nullable = false, length = 100)
    private String brancheCalculId;

    @Column(name = "arret_entrant_ref", nullable = false, length = 200)
    private String arretEntrantRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapping_actuel_id")
    private ToolJurisprudenceMapping mappingActuel;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private JurisprudenceWatchFlagSource source;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "explication", length = 2000)
    private String explication;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private JurisprudenceWatchFlagStatut statut;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 20)
    private JurisprudenceWatchFlagDecision decision;

    @Column(name = "comment_user", length = 2000)
    private String commentUser;

    @PrePersist
    void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.statut == null) {
            this.statut = JurisprudenceWatchFlagStatut.PENDING;
        }
    }
}
