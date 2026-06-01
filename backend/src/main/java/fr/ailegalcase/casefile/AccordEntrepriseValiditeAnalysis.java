package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-31 : entity 1:1 par dossier portant l'analyse de validité d'un accord
 * d'entreprise au regard des conditions de majorité (art. L.2232-12 CT ;
 * L.2261-7 et s. CT, F-DT-67). Outil single-country FR.
 */
@Entity
@Table(name = "accord_entreprise_validite_analyses")
@Getter
@Setter
public class AccordEntrepriseValiditeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "pourcentage_suffrages_signataires", nullable = false, precision = 5, scale = 2)
    private BigDecimal pourcentageSuffragesSignataires;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_operation", nullable = false, length = 20)
    private AccordTypeOperation typeOperation;

    @Column(name = "referendum_organise", nullable = false)
    private boolean referendumOrganise;

    @Column(name = "referendum_approuve", nullable = false)
    private boolean referendumApprouve;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_majorite", nullable = false, length = 20)
    private AccordConditionMajorite conditionMajorite;

    @Column(name = "date_denonciation")
    private LocalDate dateDenonciation;

    @Column(name = "date_fin_survie")
    private LocalDate dateFinSurvie;

    @Column(name = "items_non_conformes", nullable = false)
    private int itemsNonConformes;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private AccordEntrepriseValiditeStatut statut;

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
