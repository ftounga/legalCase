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
 * SF-219-14 : entity 1:1 par dossier portant l'analyse <i>statut
 * intérim BE — CCT n° 322</i> (Loi du 24/07/1987 + CCT n° 322 du
 * 14/06/2010 du CNT — validité d'une mission tripartite ETI /
 * utilisateur / intérimaire sous l'angle motif autorisé + durée
 * + parité salariale + formalisme).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, ventilation
 * d'éligibilité par dimension, jours excédentaires éventuels, écart
 * de parité salariale) en JSON dans {@code result_data} — pattern
 * uniforme avec les autres outils décisionnels BE (miroir
 * {@link FlexiJobBeAnalysis} SF-219-12).</p>
 */
@Entity
@Table(name = "interim_be_cct_322_analyses")
@Getter
@Setter
public class InterimBeCct322Analysis {

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
