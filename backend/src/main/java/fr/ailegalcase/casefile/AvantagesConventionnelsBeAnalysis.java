package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-DT-28-01 : entity 1:1 par dossier portant l'analyse des avantages conventionnels belges
 * (pécule de vacances + prime fin d'année + éco-chèques + chèques-repas).
 */
@Entity
@Table(name = "avantages_conventionnels_be_analyses")
@Getter
@Setter
public class AvantagesConventionnelsBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "salaire_mensuel_brut_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireMensuelBrutEur;

    @Column(name = "jours_travailles_annee_precedente", nullable = false)
    private int joursTravaillesAnneePrecedente;

    @Column(name = "anciennette_mois", nullable = false)
    private int anciennetteMois;

    @Column(name = "commission_paritaire", nullable = false, length = 30)
    private String commissionParitaire;

    @Column(name = "annee", nullable = false)
    private int annee;

    @Column(name = "double_pecule_vacances_percu", nullable = false)
    private boolean doublePeculeVacancesPercu;

    @Column(name = "prime_fin_annee_prevue_cc_cp", nullable = false)
    private boolean primeFinAnneePrevueCcCp;

    @Column(name = "eco_cheques_prevu_cc_cp", nullable = false)
    private boolean ecoChequesPrevuCcCp;

    @Column(name = "eco_cheques_utilisation_dans_an", nullable = false)
    private boolean ecoChequesUtilisationDansAn;

    @Column(name = "cheques_repas_prevu", nullable = false)
    private boolean chequesRepasPrevu;

    @Column(name = "jours_prestes_effectifs", nullable = false)
    private int joursPrestesEffectifs;

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
