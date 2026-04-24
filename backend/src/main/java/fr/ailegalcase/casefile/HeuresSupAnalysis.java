package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-DT-19-01 : entity 1:1 par dossier portant l'analyse de rappel pour heures
 * supplémentaires (FR L.3121-28 et s. / BE art. 29 Loi 16/3/1971).
 */
@Entity
@Table(name = "heures_sup_analyses")
@Getter
@Setter
public class HeuresSupAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "taux_horaire_brut", nullable = false, precision = 8, scale = 2)
    private BigDecimal tauxHoraireBrut;

    @Column(name = "heures_sup_25pct", nullable = false)
    private int heuresSup25pct;

    @Column(name = "heures_sup_50pct", nullable = false)
    private int heuresSup50pct;

    @Column(name = "heures_hors_contingent", nullable = false)
    private int heuresHorsContingent;

    @Column(name = "taux_majoration_25", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxMajoration25;

    @Column(name = "taux_majoration_50", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxMajoration50;

    @Column(name = "heures_sup_semaine_be", nullable = false)
    private int heuresSupSemaineBe;

    @Column(name = "heures_dimanche_jf_be", nullable = false)
    private int heuresDimancheJfBe;

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
