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
 * SF-213-07 : entity 1:1 par dossier portant l'analyse de la procédure
 * interne BE de plainte pour harcèlement moral / sexuel
 * (Loi 04/08/1996 art. 32bis-32sexies + AR 10/04/2014).
 *
 * <p>Snapshot complet (inputs + outputs + checklist) en JSON dans
 * {@code result_data} — pattern uniforme avec les autres outils
 * décisionnels BE (miroir {@link TransactionBeTravailAnalysis} SF-213-06).</p>
 */
@Entity
@Table(name = "harcelement_be_procedure_formelle_analyses")
@Getter
@Setter
public class HarcelementBeProcedureFormelleAnalysis {

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
