package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-DT-12-01 : entity 1:1 par dossier portant l'analyse des dommages-intérêts
 * pour discrimination (art. L.1134-5 FR / Loi 10 mai 2007 BE).
 */
@Entity
@Table(name = "discrimination_analyses")
@Getter
@Setter
public class DiscriminationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "salaire_mensuel_reference", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireMensuelReference;

    @Column(name = "motif_discrimination", nullable = false, length = 50)
    private String motifDiscrimination;

    @Column(name = "contexte_acte", nullable = false, length = 40)
    private String contexteActe;

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
