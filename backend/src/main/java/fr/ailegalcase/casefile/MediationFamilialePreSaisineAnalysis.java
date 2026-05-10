package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-210-01 : entité 1:1 par dossier portant l'analyse de médiation familiale
 * obligatoire pré-saisine JAF (Cciv art. 373-2-10 al. 3 + Loi 18/11/2016 +
 * CPC art. 1108). Outil single-country FR.
 */
@Entity
@Table(name = "mediation_familiale_pre_saisine_analyses")
@Getter
@Setter
public class MediationFamilialePreSaisineAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "motif_saisine", nullable = false, length = 40)
    private String motifSaisine;

    @Column(name = "mediation_tentee", nullable = false)
    private boolean mediationTentee;

    @Column(name = "date_mediation")
    private LocalDate dateMediation;

    @Column(name = "exception_applicable", nullable = false, length = 40)
    private String exceptionApplicable;

    @Column(name = "exception_detail", columnDefinition = "TEXT")
    private String exceptionDetail;

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
