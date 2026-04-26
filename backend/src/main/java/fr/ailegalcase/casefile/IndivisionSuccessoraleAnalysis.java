package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-24-11 : entity 1:1 par dossier portant l'analyse de gestion d'une
 * indivision successorale (art. 815 à 832-2 + 1873-1 et s. Cciv).
 * Outil single-country FR (DROIT_FAMILLE).
 */
@Entity
@Table(name = "indivision_successorale_analyses")
@Getter
@Setter
public class IndivisionSuccessoraleAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_ouverture_succession", nullable = false)
    private LocalDate dateOuvertureSuccession;

    @Column(name = "type_indivision", nullable = false, length = 40)
    private String typeIndivision;

    @Column(name = "nb_heritiers", nullable = false)
    private int nbHeritiers;

    @Column(name = "valeur_patrimoine_indivis_eur", precision = 14, scale = 2, nullable = false)
    private BigDecimal valeurPatrimoineIndivisEur;

    @Column(name = "valeur_bien_occupe_eur", precision = 14, scale = 2, nullable = false)
    private BigDecimal valeurBienOccupeEur;

    @Column(name = "consentements_tous", nullable = false)
    private boolean consentementsTous;

    @Column(name = "occupation_exclusive", nullable = false)
    private boolean occupationExclusive;

    @Column(name = "actes_administration_contestes", nullable = false)
    private boolean actesAdministrationContestes;

    @Column(name = "demande_partage", nullable = false)
    private boolean demandePartage;

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
