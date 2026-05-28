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
 * SF-219-23 : entity 1:1 par dossier portant l'analyse <i>refus
 * d'aménagements raisonnables handicap BE</i> (Loi du 10/05/2007
 * tendant à lutter contre certaines formes de discrimination M.B.
 * 30/05/2007, art. 4 9° + art. 14 + art. 28 indemnité forfaitaire de
 * 6 mois ; CCT n° 95 du 10/10/2008 du CNT ; Directive 2000/78/CE
 * art. 5 ; Convention ONU 13/12/2006 art. 5 et 27).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, ventilation par
 * dimension qualification handicap / demande / réponse employeur /
 * charge disproportionnée / représailles / sanctions financières
 * indicatives) en JSON dans {@code result_data} — pattern uniforme
 * avec les autres outils décisionnels BE (miroir
 * {@link EgaliteFemmesHommesBeAnalysis} SF-219-22).</p>
 */
@Entity
@Table(name = "discrimination_be_handicap_amenagement_analyses")
@Getter
@Setter
public class DiscriminationBeHandicapAmenagementAnalysis {

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
