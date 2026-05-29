package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-19 : entity 1:1 par dossier portant l'analyse d'éligibilité à l'aide
 * juridictionnelle (AJ) devant la CNDA et des délais (loi n° 91-647, L. 532-4
 * CESEDA). Outil single-country FR.
 */
@Entity
@Table(name = "aj_cnda_analyses")
@Getter
@Setter
public class AjCndaAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_decision_ofpra", nullable = false)
    private LocalDate dateDecisionOfpra;

    @Column(name = "ressources_mensuelles_nettes", nullable = false)
    private double ressourcesMensuellesNettes;

    @Column(name = "procedure_acceleree", nullable = false)
    private boolean procedureAcceleree;

    @Column(name = "demande_aj_deposee", nullable = false)
    private boolean demandeAjDeposee;

    @Column(name = "date_depot_aj")
    private LocalDate dateDepotAj;

    @Column(name = "eligible_aj", nullable = false)
    private boolean eligibleAj;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private AjCndaStatut statut;

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
