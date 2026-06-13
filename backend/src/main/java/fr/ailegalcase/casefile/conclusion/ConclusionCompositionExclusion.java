package fr.ailegalcase.casefile.conclusion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * F-288 / SF-288-01 — item exclu de la composition d'un acte de conclusions.
 *
 * <p>Une ligne = un {@code item_key} (vague 1 : le {@code toolId} d'un outil
 * décisionnel CALCULÉ) que l'avocat <b>ne verse pas</b> dans l'acte, pour un
 * dossier donné et une {@code dimension}. On ne stocke que les <b>exclus</b> :
 * l'absence de ligne = comportement actuel (tous les items inclus), ce qui garantit
 * la non-régression.</p>
 *
 * <p>Table générique ({@code dimension} + {@code item_key}) prête pour les vagues 2
 * (chefs de demande) et 3 (moyens adverses). Isolation workspace via le dossier
 * (rattachement par {@code case_file_id}).</p>
 *
 * <p>Aucune logique métier dans l'entité (coding-rules).</p>
 */
@Entity
@Table(name = "conclusion_composition_exclusion")
@Getter
@Setter
public class ConclusionCompositionExclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_file_id", nullable = false)
    private UUID caseFileId;

    @Column(name = "dimension", nullable = false, length = 40)
    private String dimension;

    @Column(name = "item_key", nullable = false, length = 120)
    private String itemKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
