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
 * SF-219-05 : entity 1:1 par dossier portant l'analyse de conformité de
 * l'outplacement BE général au titre du régime "préavis ≥ 30 semaines"
 * (Loi du 05/09/2001 art. 11 + AR du 21/10/2007).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, conformité par condition,
 * indemnité forfaitaire indicative) en JSON dans {@code result_data} —
 * pattern uniforme avec les autres outils décisionnels BE (miroir
 * {@link RccBeEntrepriseDifficulteAnalysis} SF-219-03 et
 * {@link CumulRccAllocationsAnalysis} SF-219-04).</p>
 */
@Entity
@Table(name = "outplacement_be_general_30sem_analyses")
@Getter
@Setter
public class OutplacementBeGeneral30semAnalysis {

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
