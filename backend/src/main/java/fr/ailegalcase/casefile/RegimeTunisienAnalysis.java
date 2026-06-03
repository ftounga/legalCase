package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-220-01 : entity 1:1 par dossier portant l'analyse du régime franco-tunisien
 * (accord du 17/03/1988). Outil single-country FR (F-IM-47-regime-tunisien-fr).
 */
@Entity
@Table(name = "regime_tunisien_analyses")
@Getter
@Setter
public class RegimeTunisienAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "categorie", nullable = false, length = 20)
    private String categorie;

    @Column(name = "duree_sejour_envisagee_mois")
    private Integer dureeSejourEnvisageeMois;

    @Column(name = "titre_en_cours", nullable = false)
    private boolean titreEnCours;

    @Column(name = "deja_resident", nullable = false)
    private boolean dejaResident;

    @Column(name = "country", nullable = false, length = 20)
    private String country;

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
