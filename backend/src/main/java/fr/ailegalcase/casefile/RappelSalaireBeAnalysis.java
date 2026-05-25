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
 * SF-213-02 : entity 1:1 par dossier portant l'analyse de rappel de salaire en
 * droit belge du travail — calcul d'intérêts moratoires (10 %, Loi du
 * 12/04/1965 art. 10) et de prescription (1 an post-rupture / 5 ans pendant le
 * contrat — Loi du 03/07/1978 art. 15).
 *
 * <p>Snapshot complet (inputs + outputs) en JSON dans {@code result_data} —
 * pattern uniforme avec les autres outils décisionnels BE (pattern miroir
 * {@link ClauseNonConcurrenceBeAnalysis} / {@link RccBeConditionsAnalysis}).</p>
 */
@Entity
@Table(name = "rappel_salaire_be_analyses")
@Getter
@Setter
public class RappelSalaireBeAnalysis {

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
