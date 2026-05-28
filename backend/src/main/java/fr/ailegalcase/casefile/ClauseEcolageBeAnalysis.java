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
 * SF-219-17 : entity 1:1 par dossier portant l'analyse <i>clause
 * d'écolage BE</i> (art. 22bis Loi du 03/07/1978 sur les contrats de
 * travail, introduit par la Loi du 27/12/2006, M.B. 28/12/2006 —
 * conditions de validité d'une clause d'écolage et calcul du
 * remboursement dégressif).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, quotité due,
 * plafond 80 %, montant dû final) en JSON dans {@code result_data} —
 * pattern uniforme avec les autres outils décisionnels BE (miroir
 * {@link ClauseNonConcurrenceBeAnalysis} SF-213-01).</p>
 */
@Entity
@Table(name = "clause_ecolage_be_analyses")
@Getter
@Setter
public class ClauseEcolageBeAnalysis {

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
