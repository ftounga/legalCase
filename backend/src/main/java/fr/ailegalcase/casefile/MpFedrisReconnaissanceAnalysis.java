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
 * SF-219-28 : entity 1:1 par dossier portant l'analyse <i>Reconnaissance
 * d'une maladie professionnelle Fedris BE</i> (Lois coordonnées du
 * 03/06/1970 + AR du 28/03/1969 liste fermée + AR du 16/12/1985 système
 * ouvert + Loi du 11/01/2018 réformant Fedris).
 *
 * <p>Snapshot complet (inputs + outputs : verdict reconnaissance, base
 * juridique, voies de recours, prescription triennale, conséquences
 * indemnitaires) en JSON dans {@code result_data} — pattern uniforme
 * avec les autres outils décisionnels BE F-219 (miroir
 * {@link TravailNoirBeDimonaAnalysis} SF-219-26).</p>
 */
@Entity
@Table(name = "mp_fedris_reconnaissance_analyses")
@Getter
@Setter
public class MpFedrisReconnaissanceAnalysis {

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
