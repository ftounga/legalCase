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
 * SF-219-11 : entity 1:1 par dossier portant l'analyse <i>congé-éducation
 * payé régionalisé</i> (Loi 22/01/1985 régionalisée 2014 — WBR / FLA / BXL).
 *
 * <p>L'outil unique « conge-education-paye-region » est un arbre
 * décisionnel à branchement régional interne (Wallonie : Décret WBR
 * 19/02/2014 ; Flandre : VOV Décret 12/12/2014 ; Bruxelles : Ordonnance
 * 02/07/2015) — la persistance se fait dans une table unique pour
 * permettre la restitution UI homogène (snapshot complet inputs +
 * outputs en JSON dans {@code result_data}).</p>
 *
 * <p>Miroir {@link DelegueSyndicalCct5Analysis} SF-219-10 pour le
 * pattern de persistance.</p>
 */
@Entity
@Table(name = "conge_education_paye_region_analyses")
@Getter
@Setter
public class CongeEducationPayeRegionAnalysis {

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
