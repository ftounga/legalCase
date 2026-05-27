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
 * SF-213-10 : entity 1:1 par dossier portant l'analyse du score CCT n° 109
 * (licenciement manifestement déraisonnable, art. 9 CCT 12/02/2014 — outil
 * BELGIQUE uniquement).
 *
 * <p>Snapshot complet (inputs + outputs : échelon, indemnité, justification,
 * avertissement cumul ICP) en JSON dans {@code result_data} — pattern uniforme
 * avec les autres outils décisionnels BE de la F-213 (miroir
 * {@link LicenciementBeProtectionDelegueeAnalysis} SF-213-08).</p>
 */
@Entity
@Table(name = "licenciement_be_cct109_deraisonnable_analyses")
@Getter
@Setter
public class LicenciementBeCct109DeraisonnableAnalysis {

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
