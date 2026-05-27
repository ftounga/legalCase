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
 * SF-219-01 : entity 1:1 par dossier portant l'analyse d'éligibilité au
 * RCC métiers lourds BE (CCT 17 + AR 03/05/2007 art. 3 — conditions
 * âge ≥ 58 / carrière ≥ 35 / au moins 5/10 ou 7/15 ans de métier lourd).
 *
 * <p>Snapshot complet (inputs + outputs : verdict + raison + synthèse) en
 * JSON dans {@code result_data} — pattern uniforme avec les autres outils
 * décisionnels BE.</p>
 */
@Entity
@Table(name = "rcc_be_metiers_lourds_analyses")
@Getter
@Setter
public class RccBeMetiersLourdsAnalysis {

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
