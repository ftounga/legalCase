package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-20-01 : entity 1:1 par dossier portant l'analyse de rappel de salaire FR
 * (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail + CCN).
 */
@Entity
@Table(name = "rappel_salaire_analyses")
@Getter
@Setter
public class RappelSalaireAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "periode_debut", nullable = false)
    private LocalDate periodeDebut;

    @Column(name = "periode_fin", nullable = false)
    private LocalDate periodeFin;

    @Column(name = "montant_salaire_du_mensuel_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantSalaireDuMensuelEur;

    @Column(name = "montant_salaire_per_verse_mensuel_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantSalairePerVerseMensuelEur;

    @Column(name = "convention_collective_code", length = 30)
    private String conventionCollectiveCode;

    @Column(name = "anciennete_annees_prime", nullable = false)
    private int ancienneteAnneesPrime;

    @Column(name = "index_insee_revalorise", nullable = false)
    private boolean indexInseeRevalorise;

    @Column(name = "taux_revalorisation_pct", precision = 6, scale = 2)
    private BigDecimal tauxRevalorisationPct;

    @Column(name = "methode_cp_sur_rappel", length = 20, nullable = false)
    private String methodeCpSurRappel;

    @Column(name = "total_avec_cp_eur", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAvecCpEur;

    @Column(name = "country", length = 20, nullable = false)
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
