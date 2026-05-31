package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-29 : entity 1:1 par dossier portant l'analyse de conformité de la
 * négociation annuelle obligatoire (NAO, art. L.2242-1 à L.2242-8 CT, F-DT-66).
 * Outil single-country FR.
 */
@Entity
@Table(name = "nao_negociation_annuelle_analyses")
@Getter
@Setter
public class NaoNegociationAnnuelleAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "effectif", nullable = false)
    private int effectif;

    @Column(name = "delegue_syndical_present", nullable = false)
    private boolean delegueSyndicalPresent;

    @Column(name = "applicable", nullable = false)
    private boolean applicable;

    @Column(name = "periodicite_mois", nullable = false)
    private int periodiciteMois;

    @Column(name = "date_prochaine_echeance")
    private LocalDate dateProchaineEcheance;

    @Column(name = "jours_avant_echeance")
    private Integer joursAvantEcheance;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_echeance", length = 20)
    private NaoStatutEcheance statutEcheance;

    @Column(name = "items_obligatoires_manquants", nullable = false)
    private int itemsObligatoiresManquants;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private NaoNegociationAnnuelleStatut statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "risque_entrave", nullable = false, length = 10)
    private NaoRisqueEntrave risqueEntrave;

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
