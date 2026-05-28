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
 * SF-219-18 : entity 1:1 par dossier portant l'analyse <i>semaine de 4
 * jours BE</i> (Loi du 03/10/2022 « Deal pour l'emploi » M.B.
 * 10/11/2022, art. 5 — possibilité offerte au travailleur à temps
 * plein de demander la compression du temps de travail hebdomadaire
 * sur 4 jours, sans réduction de la durée hebdomadaire de travail).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, ventilation par
 * dimension demande/accord/durée/journée/protection) en JSON dans
 * {@code result_data} — pattern uniforme avec les autres outils
 * décisionnels BE (miroir
 * {@link TeletravailBeCct85149Analysis} SF-219-16).</p>
 */
@Entity
@Table(name = "semaine_4_jours_be_analyses")
@Getter
@Setter
public class Semaine4JoursBeAnalysis {

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
