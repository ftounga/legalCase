package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-211-01 : entity 1:1 par dossier portant l'analyse du divorce par consentement
 * mutuel belge (CJ art. 1287+ et Loi 27/04/2007). Outil single-country BE.
 */
@Entity
@Table(name = "divorce_dc_be_analyses")
@Getter
@Setter
public class DivorceDcBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_signature_convention", nullable = false)
    private LocalDate dateSignatureConvention;

    @Column(name = "date_audience_homologation")
    private LocalDate dateAudienceHomologation;

    @Column(name = "convention_logement", nullable = false)
    private boolean conventionLogement;

    @Column(name = "convention_biens", nullable = false)
    private boolean conventionBiens;

    @Column(name = "convention_garde_enfants", nullable = false)
    private boolean conventionGardeEnfants;

    @Column(name = "convention_contributions", nullable = false)
    private boolean conventionContributions;

    @Column(name = "enfants_mineurs_communs", nullable = false)
    private boolean enfantsMineursCommuns;

    @Column(name = "epoux_consentent", nullable = false)
    private boolean epouxConsentent;

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
