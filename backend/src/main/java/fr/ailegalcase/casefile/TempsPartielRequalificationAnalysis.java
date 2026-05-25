package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-212-33 : entity 1:1 par dossier portant la dernière analyse de
 * requalification d'un contrat à temps partiel en temps complet
 * (F-DT-49-temps-partiel-requalification, FRANCE — L. 3123-1 à
 * L. 3123-20 CT, L. 3123-6 mentions obligatoires, L. 3123-9 heures
 * complémentaires, L. 3245-1 CT prescription rappel salaire 3 ans,
 * Cass. soc. 22/01/1992 présomption de temps complet réfragable).
 *
 * <p>Stocke un snapshot JSON complet (inputs + résultat) pour permettre
 * la restitution de l'écran de l'avocat tel qu'il l'avait laissé
 * (pattern aligné sur F-DT-42 / SF-206-01 / SF-212-01 / SF-212-27).</p>
 */
@Entity
@Table(name = "temps_partiel_requalification_analyses")
@Getter
@Setter
public class TempsPartielRequalificationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    /** Snapshot JSON sérialisé (inputs + outputs) — source de vérité pour le GET. */
    @Column(name = "snapshot_data", nullable = false, columnDefinition = "TEXT")
    private String snapshotData = "{}";

    @Column(name = "country", nullable = false, length = 20)
    private String country;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
