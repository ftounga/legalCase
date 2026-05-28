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
 * SF-219-10 : entity 1:1 par dossier portant l'analyse <i>statut du
 * délégué syndical</i> (CCT n° 5 du 24/05/1971 conclue au Conseil
 * National du Travail — statut des délégations syndicales du personnel
 * des entreprises). Distinct de SF-213-08 (protection contre le
 * licenciement de la délégation, Loi du 19/03/1991) : ici on couvre le
 * <b>statut</b> général (création, missions, désignation, durée du
 * mandat, crédit d'heures, facilités).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, missions exerçables,
 * conditions de désignation) en JSON dans {@code result_data} —
 * pattern uniforme avec les autres outils décisionnels BE (miroir
 * {@link TransfertEntrepriseCct32bisAnalysis} SF-219-08).</p>
 */
@Entity
@Table(name = "delegue_syndical_cct_5_analyses")
@Getter
@Setter
public class DelegueSyndicalCct5Analysis {

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
