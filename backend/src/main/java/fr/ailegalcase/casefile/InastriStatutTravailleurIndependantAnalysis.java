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
 * SF-219-27 : entity 1:1 par dossier portant l'analyse <i>INASTI —
 * statut travailleur indépendant et qualification salarié /
 * indépendant</i> (Loi du 27/06/1969 + AR n° 38 du 27/07/1967 +
 * Loi-programme I du 27/12/2006 art. 328 à 333 + critères sectoriels
 * art. 337/2).
 *
 * <p>Snapshot complet (inputs + outputs : verdict de qualification,
 * scores critères généraux, drapeaux présomption sectorielle /
 * générale, drapeau requalification recommandée) en JSON dans
 * {@code result_data} — pattern uniforme avec les autres outils
 * décisionnels BE (miroir {@link TravailNoirBeDimonaAnalysis} SF-219-26).</p>
 */
@Entity
@Table(name = "inastri_statut_travailleur_independant_analyses")
@Getter
@Setter
public class InastriStatutTravailleurIndependantAnalysis {

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
