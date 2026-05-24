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
 * SF-212-19 : entity 1:1 par dossier portant la dernière analyse de la
 * régularité d'une mise à pied disciplinaire
 * (F-DT-48-mise-a-pied-disciplinaire, FRANCE — L. 1331-1 CT ; L. 1332-1
 * à L. 1332-4 CT ; Cass. soc. constante).
 *
 * <p>Stocke un snapshot JSON complet (inputs + résultat) pour permettre la
 * restitution de l'écran de l'avocat tel qu'il l'avait laissé.</p>
 */
@Entity
@Table(name = "mise_a_pied_disciplinaire_analyses")
@Getter
@Setter
public class MiseAPiedDisciplinaireAnalysis {

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
