package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-17 : entity 1:1 par dossier portant l'analyse de la procédure
 * d'introduction de la demande d'asile à l'OFPRA (GUDA / ADA,
 * art. R. 521-1 et s. CESEDA). Outil single-country FR.
 */
@Entity
@Table(name = "ofpra_introduction_analyses")
@Getter
@Setter
public class OfpraIntroductionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_arrivee_en_france", nullable = false)
    private LocalDate dateArriveeEnFrance;

    @Column(name = "passage_guda_effectue", nullable = false)
    private boolean passageGudaEffectue;

    @Column(name = "date_passage_guda")
    private LocalDate datePassageGuda;

    @Column(name = "ada_requise", nullable = false)
    private boolean adaRequise;

    @Column(name = "pays_origine", length = 120)
    private String paysOrigine;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_delai", nullable = false, length = 20)
    private OfpraIntroductionStatut statutDelai;

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
