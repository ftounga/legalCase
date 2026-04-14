package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "anciennete_analyses")
@Getter
@Setter
public class AncienneteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(nullable = false, length = 30)
    private String conventionCode;

    @Column(nullable = false)
    private LocalDate dateEntree;

    @Column(name = "salaire_base", precision = 12, scale = 2)
    private BigDecimal salaireBase;

    @Column(name = "conges_contrat")
    private Integer congesContrat;

    @Column(name = "prime_contrat", precision = 5, scale = 2)
    private BigDecimal primeContrat;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resultData = "{}";

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
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
