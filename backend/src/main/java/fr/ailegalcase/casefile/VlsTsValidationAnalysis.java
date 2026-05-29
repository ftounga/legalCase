package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-07 : entity 1:1 par dossier portant l'analyse du délai de validation du
 * VLS-TS auprès de l'OFII (art. R. 311-3 CESEDA). Outil single-country FR.
 */
@Entity
@Table(name = "vls_ts_validation_analyses")
@Getter
@Setter
public class VlsTsValidationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_entree_france", nullable = false)
    private LocalDate dateEntreeFrance;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_vls_ts", nullable = false, length = 30)
    private VlsTsValidationTypeEnum typeVlsTs;

    @Column(name = "validation_ofii_effectuee", nullable = false)
    private boolean validationOFIIEffectuee;

    @Column(name = "date_validation_ofii")
    private LocalDate dateValidationOFII;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private VlsTsValidationStatut statut;

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
