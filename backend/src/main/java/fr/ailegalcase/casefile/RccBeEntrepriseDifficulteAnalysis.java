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
 * SF-219-03 : entity 1:1 par dossier portant l'analyse RCC BE entreprise
 * en difficulté / restructuration (Loi du 26/12/2013 + CCT n° 17 du
 * 19/12/1974 + AR du 03/05/2007 + AR de reconnaissance ministérielle —
 * conditions âge réduit (≥ 55 ans typique) / carrière ≥ 10 ans /
 * ancienneté secteur ≥ 5 ans + reconnaissance ministre obligatoire).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, conditions cumulatives,
 * indemnité complémentaire indicative) en JSON dans {@code result_data} —
 * pattern uniforme avec les autres outils décisionnels BE (miroir
 * {@link RccBeLongueCarriereAnalysis} SF-219-02 et
 * {@link RccBeMetiersLourdsAnalysis} SF-219-01).</p>
 */
@Entity
@Table(name = "rcc_be_entreprise_difficulte_analyses")
@Getter
@Setter
public class RccBeEntrepriseDifficulteAnalysis {

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
