package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-19-01 : entity 1:1 par dossier portant l'analyse d'éligibilité d'un mineur étranger
 * à l'un des 4 dispositifs (MNA art. 375 Cciv + L.221-2-2 CASF, L.435-3 CESEDA, DCEM R.321-3,
 * TIR R.321-7). Outil single-country FR.
 */
@Entity
@Table(name = "mineurs_immigration_analyses")
@Getter
@Setter
public class MineursImmigrationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "dispositif_vise", nullable = false, length = 40)
    private String dispositifVise;

    @Column(name = "date_naissance", nullable = false)
    private LocalDate dateNaissance;

    @Column(name = "date_entree_france")
    private LocalDate dateEntreeFrance;

    @Column(name = "parent_regulier", nullable = false)
    private boolean parentRegulier;

    @Column(name = "isolement_avere", nullable = false)
    private boolean isolementAvere;

    @Column(name = "motif_ordre_public", nullable = false)
    private boolean motifOrdrePublic;

    @Column(name = "nationalite", length = 60)
    private String nationalite;

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
