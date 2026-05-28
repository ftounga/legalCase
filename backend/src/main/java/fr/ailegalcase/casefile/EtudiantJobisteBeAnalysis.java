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
 * SF-219-13 : entity 1:1 par dossier portant l'analyse <i>étudiant
 * jobiste BE</i> (Loi du 03/07/1978 + Loi-programme 24/12/2002 + AR
 * 14/07/1995 + Loi-programme 22/12/2023 — quota 600h/an + cotisations
 * réduites + formalisme contrat écrit + Dimona STU).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, heures restantes,
 * heures hors quota, redressement estimé, ventilation d'éligibilité)
 * en JSON dans {@code result_data} — pattern uniforme avec les autres
 * outils décisionnels BE (miroir {@link FlexiJobBeAnalysis}
 * SF-219-12).</p>
 */
@Entity
@Table(name = "etudiant_jobiste_be_analyses")
@Getter
@Setter
public class EtudiantJobisteBeAnalysis {

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
