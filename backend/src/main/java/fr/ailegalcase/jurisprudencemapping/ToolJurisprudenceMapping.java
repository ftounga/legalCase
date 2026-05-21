package fr.ailegalcase.jurisprudencemapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-01 — mapping curaté entre une branche de calcul d'un outil
 * décisionnel et un arrêt structurant qui fonde juridiquement le calcul.
 *
 * <p>Table GLOBALE (pas d'isolation workspace) — la jurisprudence est identique
 * pour tous les avocats. {@code chapeau_officiel} est le texte brut publié par
 * la juridiction (Cour de cassation, Conseil d'État, Cour const. BE, etc.) —
 * pas une reformulation Claude, garantie d'absence de déformation.</p>
 *
 * <p>Use case orthogonal à F-242 {@code case_jurisprudence_citations}
 * (saisie manuelle avocat per-point juridique d'un dossier) — voir
 * {@code SF-JU-01-01-backend-infrastructure.md} Note 11.</p>
 */
@Entity
@Table(name = "tool_jurisprudence_mappings")
@Getter
@Setter
public class ToolJurisprudenceMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tool_id", nullable = false, length = 100)
    private String toolId;

    @Column(name = "branche_calcul_id", nullable = false, length = 100)
    private String brancheCalculId;

    @Column(name = "arret_ref", nullable = false, length = 200)
    private String arretRef;

    @Column(name = "juridiction", nullable = false, length = 50)
    private String juridiction;

    @Column(name = "date_arret", nullable = false)
    private LocalDate dateArret;

    @Column(name = "numero_pourvoi", nullable = false, length = 50)
    private String numeroPourvoi;

    @Column(name = "lien_legifrance", nullable = false, length = 500)
    private String lienLegifrance;

    @Column(name = "chapeau_officiel", nullable = false, length = 2000)
    private String chapeauOfficiel;

    @Column(name = "last_verified_at", nullable = false)
    private Instant lastVerifiedAt;

    @Column(name = "confidence_score", nullable = false, precision = 3, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @PrePersist
    void onPrePersist() {
        if (this.lastVerifiedAt == null) {
            this.lastVerifiedAt = Instant.now();
        }
    }
}
