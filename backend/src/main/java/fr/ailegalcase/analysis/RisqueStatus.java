package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.workspace.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-195 SF-195-01 — Statut avocat trichotomique (`A_CREUSER` / `VALIDE` /
 * `ECARTE`) appliqué à un risque extrait par l'IA dans
 * {@code case_analyses.analysis_result.risques}.
 *
 * <p>Overlay sur le tableau JSON {@code risques} existant (Option A retenue
 * 2026-05-06, miroir F-194 SF-194-01) : la source de vérité IA reste le
 * tableau JSON brut, ce statut s'y joint au runtime via le libellé normalisé
 * {@code (case_file_id, risque_libelle_normalise)} pour préserver F-IA-02
 * strictement (le {@code score_risque} IA brut n'est pas modifié — F-195
 * produit un {@code score_risque_avocat} parallèle).</p>
 *
 * <p>Le statut par défaut implicite (sans entrée table) = {@code A_CREUSER}.</p>
 *
 * <p>Pattern miroir {@link PieceManquanteStatus} (F-194) — le PUT statut reste
 * un acte pur, tous les effets matérialisés au prochain run de Synthèse
 * enrichie par {@link RisqueAlignmentService#materializeForAnalysis}.</p>
 */
@Entity
@Table(name = "risque_status",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_risque_status_case_file_norm",
                columnNames = {"case_file_id", "risque_libelle_normalise"}))
@Getter
@Setter
public class RisqueStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "risque_libelle_normalise", nullable = false, length = 500)
    private String risqueLibelleNormalise;

    @Column(name = "risque_libelle_original", nullable = false, length = 500)
    private String risqueLibelleOriginal;

    @Column(name = "statut", nullable = false, length = 20)
    private String statut;

    @Column(name = "raison_ecarte", columnDefinition = "TEXT")
    private String raisonEcarte;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Statuts trichotomiques cohérents F-194 (équivalent A_DEMANDER / OBTENUE / NON_APPLICABLE). */
    public static final String STATUT_A_CREUSER = "A_CREUSER";
    public static final String STATUT_VALIDE = "VALIDE";
    public static final String STATUT_ECARTE = "ECARTE";

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
