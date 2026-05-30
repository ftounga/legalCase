package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-13 : entity 1:1 par dossier portant l'analyse de la rupture du contrat
 * d'un salarié du particulier employeur (CESU / assistant maternel) — préavis et
 * indemnité de licenciement / de rupture. Outil single-country FR.
 */
@Entity
@Table(name = "particulier_employeur_cesu_analyses")
@Getter
@Setter
public class ParticulierEmployeurCesuAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_entree", nullable = false)
    private LocalDate dateEntree;

    @Column(name = "date_rupture", nullable = false)
    private LocalDate dateRupture;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_employe", nullable = false, length = 40)
    private ParticulierEmployeurCesuCategorie categorieEmploye;

    @Enumerated(EnumType.STRING)
    @Column(name = "cause_rupture", nullable = false, length = 40)
    private ParticulierEmployeurCesuCause causeRupture;

    @Column(name = "salaire_mensuel_moyen", nullable = false)
    private BigDecimal salaireMensuelMoyen;

    @Column(name = "duree_preavis_jours", nullable = false)
    private int dureePreavisJours;

    @Column(name = "indemnite_licenciement", nullable = false)
    private BigDecimal indemniteLicenciement;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibilite_indemnite", nullable = false, length = 20)
    private ParticulierEmployeurCesuEligibilite eligibiliteIndemnite;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 30)
    private ParticulierEmployeurCesuVerdict verdict;

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
