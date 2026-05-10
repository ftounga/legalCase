package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-210-03 : entité 1:1 par dossier portant l'analyse de choix d'option
 * successorale (Cciv art. 768+). Outil single-country FR.
 */
@Entity
@Table(name = "acceptation_renonciation_succession_analyses")
@Getter
@Setter
public class AcceptationRenonciationSuccessionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_ouverture_succession", nullable = false)
    private LocalDate dateOuvertureSuccession;

    @Column(name = "qualite_heritier", nullable = false, length = 20)
    private String qualiteHeritier = "PREMIER_RANG";

    @Column(name = "actif_brut_eur", nullable = false, precision = 14, scale = 2)
    private BigDecimal actifBrutEur = BigDecimal.ZERO;

    @Column(name = "passif_eur", nullable = false, precision = 14, scale = 2)
    private BigDecimal passifEur = BigDecimal.ZERO;

    @Column(name = "actes_equivalent_acceptation", nullable = false)
    private boolean actesEquivalentAcceptation;

    @Column(name = "inventaire_realise", nullable = false)
    private boolean inventaireRealise;

    @Column(name = "dettes_incertaines", nullable = false)
    private boolean dettesIncertaines;

    @Column(name = "intention_exprimee", length = 40)
    private String intentionExprimee;

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
