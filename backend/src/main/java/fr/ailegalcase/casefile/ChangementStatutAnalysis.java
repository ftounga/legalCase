package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-IM-11-01 : entity 1:1 par dossier portant l'analyse du changement de statut
 * (CESEDA L.421+ et R.5221). Outil single-country FR.
 */
@Entity
@Table(name = "changement_statut_analyses")
@Getter
@Setter
public class ChangementStatutAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "titre_actuel", nullable = false, length = 60)
    private String titreActuel;

    @Column(name = "titre_envisage", nullable = false, length = 60)
    private String titreEnvisage;

    @Column(name = "duree_restante_mois", nullable = false)
    private int dureeRestanteMois;

    @Column(name = "document_justificatif_fourni", nullable = false)
    private boolean documentJustificatifFourni;

    @Column(name = "remuneration_contrat_eur", precision = 12, scale = 2)
    private BigDecimal remunerationContratEur;

    @Column(name = "casier_judiciaire_vierge", nullable = false)
    private boolean casierJudiciaireVierge;

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
