package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * SF-FA-17-01 : entity 1:1 par dossier portant l'analyse de recevabilité du
 * partage judiciaire (FR).
 */
@Entity
@Table(name = "partage_judiciaire_analyses")
@Getter
@Setter
public class PartageJudiciaireAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "etape_procedure", nullable = false, length = 50)
    private PartageJudiciaireCalculator.EtapeProcedure etapeProcedure;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_bien_indivision", nullable = false, length = 30)
    private PartageJudiciaireCalculator.TypeBienIndivision typeBienIndivision;

    @Column(name = "nombre_coindivisaires", nullable = false)
    private int nombreCoindivisaires;

    @Column(name = "valeur_estimee_biens_eur", nullable = false)
    private double valeurEstimeeBiensEur;

    @Column(name = "pv_difficultes_etabli", nullable = false)
    private boolean pvDifficultesEtabli;

    @Column(name = "tentative_amiable_epuisee", nullable = false)
    private boolean tentativeAmiableEpuiseuee;

    @Column(name = "desaccord_motive", nullable = false)
    private boolean desaccordMotive;

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
