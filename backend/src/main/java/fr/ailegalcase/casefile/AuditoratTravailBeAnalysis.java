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
 * SF-219-25 : entity 1:1 par dossier portant l'analyse <i>Auditorat
 * du travail BE — orientation et checklist de saisine du parquet
 * spécialisé en droit social pénal</i> (Code judiciaire art. 138bis +
 * Code d'instruction criminelle art. 24 + Loi du 03/08/1992 sur le
 * Code judiciaire).
 *
 * <p>Snapshot complet (inputs + outputs : verdict de pertinence,
 * mode de saisine recommandé, drapeaux conditionnels procéduraux)
 * en JSON dans {@code result_data} — pattern uniforme avec les
 * autres outils décisionnels BE (miroir
 * {@link CodePenalSocialBeAnalysis} SF-219-24).</p>
 */
@Entity
@Table(name = "auditorat_travail_be_analyses")
@Getter
@Setter
public class AuditoratTravailBeAnalysis {

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
