package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-23-01 : entity 1:1 par dossier portant l'analyse de requalification
 * intérim → CDI (art. L.1251-40, L.1251-41 Code du travail).
 */
@Entity
@Table(name = "requalification_interim_cdi_analyses")
@Getter
@Setter
public class RequalificationInterimCdiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "motif_interim_invoque", nullable = false, length = 50)
    private String motifInterimInvoque;

    @Column(name = "motif_interdit", nullable = false)
    private boolean motifInterdit;

    @Column(name = "motif_interdit_type", length = 50)
    private String motifInterditType;

    @Column(name = "delai_carence_respecte", nullable = false)
    private boolean delaiCarenceRespecte;

    @Column(name = "duree_missions_totale_mois", nullable = false)
    private int dureeMissionsTotaleMois;

    @Column(name = "salaire_mensuel_brut_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireMensuelBrutEur;

    @Column(name = "date_fin_derniere_mission", nullable = false)
    private LocalDate dateFinDerniereMission;

    @Column(name = "meme_entreprise_utilisatrice", nullable = false)
    private boolean memeEntrepriseUtilisatrice;

    @Column(name = "score_requalification", nullable = false)
    private int scoreRequalification;

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
