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
 * SF-213-04 : entity 1:1 par dossier portant le calcul du préavis selon la
 * <b>Formule Claeys</b> (ancien art. 82 Loi 03/07/1978) — contrats antérieurs
 * au statut unique (01/01/2014), avec ou sans clause de sauvegarde
 * (Loi 26/12/2013 art. 67).
 *
 * <p>Snapshot complet (inputs + outputs) en JSON dans {@code result_data} —
 * pattern uniforme avec les autres outils décisionnels BE (miroir
 * {@link LicenciementBeStatutUniquePreavisAnalysis} SF-213-03).</p>
 */
@Entity
@Table(name = "licenciement_be_formule_claeys_analyses")
@Getter
@Setter
public class LicenciementBeFormuleClaeysAnalysis {

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
