package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-27-01 : entity 1:1 par dossier portant l'analyse PMA / GPA / bioéthique
 * (loi 2/8/2021 + Cass. 18/12/2022). Outil single-country FR.
 */
@Entity
@Table(name = "pma_gpa_bioethique_analyses")
@Getter
@Setter
public class PmaGpaBioethiqueAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "dispositif", nullable = false, length = 40)
    private String dispositif;

    @Column(name = "consentements_conjoints_notaire")
    private Boolean consentementsConjointsNotaire;

    @Column(name = "date_reconnaissance_anterieure_pma")
    private LocalDate dateReconnaissanceAnterieurePMA;

    @Column(name = "date_pma")
    private LocalDate datePMA;

    @Column(name = "conditions_acces_pma")
    private Boolean conditionsAccesPMA;

    @Column(name = "pays_gpa_legal")
    private Boolean paysGPALegal;

    @Column(name = "parent_biologique_avere")
    private Boolean parentBiologiqueAvere;

    @Column(name = "decision_etrangere_produite")
    private Boolean decisionEtrangereProduite;

    @Column(name = "adoption_demande")
    private Boolean adoptionDemande;

    @Column(name = "date_don")
    private LocalDate dateDon;

    @Column(name = "demande_acces")
    private Boolean demandeAcces;

    @Column(name = "age_demandeur")
    private Integer ageDemandeur;

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
