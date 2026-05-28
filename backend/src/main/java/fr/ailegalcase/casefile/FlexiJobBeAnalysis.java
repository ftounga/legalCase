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
 * SF-219-12 : entity 1:1 par dossier portant l'analyse <i>flexi-job
 * BE</i> (Loi-programme 26/12/2013 + extensions 2015/2018/2023 —
 * éligibilité d'un travailleur et d'un employeur au régime flexi-job
 * + contrôle formalisme + plafond).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, ventilation
 * d'éligibilité par dimension, excédent annuel au plafond) en JSON
 * dans {@code result_data} — pattern uniforme avec les autres outils
 * décisionnels BE (miroir {@link DelegueSyndicalCct5Analysis}
 * SF-219-10).</p>
 */
@Entity
@Table(name = "flexi_job_be_analyses")
@Getter
@Setter
public class FlexiJobBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "result_data", nullable = false, columnDefinition = "TEXT")
    private String resultData = "{}";

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
