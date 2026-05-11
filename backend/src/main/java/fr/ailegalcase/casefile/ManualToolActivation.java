package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-238-03 : enregistrement d'une activation manuelle d'un outil décisionnel
 * par un avocat sur un dossier donné. Permet à l'avocat de basculer un outil
 * du catalogue (déclenchement contextuel non rempli par l'IA) vers la grille
 * d'outils actifs sans attendre que l'analyse IA détecte la situation.
 *
 * <p>Cycle de vie :</p>
 * <ul>
 *   <li>création : {@code deactivated_at = NULL} (l'outil est actif)</li>
 *   <li>désactivation : {@code deactivated_at = now} (l'outil redescend en catalog)</li>
 * </ul>
 *
 * <p>Contrainte d'unicité partielle : un seul enregistrement avec
 * {@code deactivated_at IS NULL} par couple (case_file_id, tool_id) — garanti
 * via index unique conditionnel Postgres (migration 224).</p>
 *
 * <p>Isolation workspace : la colonne {@code workspace_id} est redondante avec
 * {@code case_file.workspace_id} mais facilite les requêtes scope-tenant et
 * les audits.</p>
 */
@Entity
@Table(name = "manual_tool_activations")
@Getter
@Setter
public class ManualToolActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_file_id", nullable = false)
    private UUID caseFileId;

    @Column(name = "tool_id", nullable = false, length = 128)
    private String toolId;

    @Column(name = "activated_by", nullable = false)
    private UUID activatedBy;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @PrePersist
    void onPrePersist() {
        if (this.activatedAt == null) {
            this.activatedAt = Instant.now();
        }
    }
}
