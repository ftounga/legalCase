package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-11-01 : entity 1:1 par dossier portant l'analyse du divorce pour
 * <b>désunion irrémédiable BE</b> (art. 229 §3 Code civil belge, Loi
 * 27/04/2007). Outil single-country BELGIQUE (DROIT_FAMILLE).
 */
@Entity
@Table(name = "divorce_desunion_irremediable_be_analyses")
@Getter
@Setter
public class DivorceDesunionIrremediableBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_separation", nullable = false)
    private LocalDate dateSeparation;

    @Column(name = "separation_consentue", nullable = false)
    private boolean separationConsentue;

    @Column(name = "preuves_separation", nullable = false)
    private boolean preuvesSeparation;

    @Column(name = "preuves_documentaires", nullable = false)
    private boolean preuvesDocumentaires;

    @Column(name = "tentatives_reconciliation", nullable = false)
    private boolean tentativesReconciliation;

    @Column(name = "date_assignation")
    private LocalDate dateAssignation;

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
