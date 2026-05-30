package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-15 : entity 1:1 par dossier portant l'analyse du statut de journaliste
 * professionnel lors d'une rupture — clause de cession / conscience, indemnité de
 * congédiement, commission arbitrale (art. L.7111-1 et s. CT). Outil
 * single-country FR.
 */
@Entity
@Table(name = "journaliste_statut_analyses")
@Getter
@Setter
public class JournalisteStatutAnalysis {

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
    @Column(name = "type_rupture", nullable = false, length = 30)
    private JournalisteStatutTypeRupture typeRupture;

    @Column(name = "salaire_mensuel_moyen", nullable = false)
    private BigDecimal salaireMensuelMoyen;

    @Column(name = "carte_identite_professionnelle", nullable = false)
    private boolean carteIdentiteProfessionnelle;

    @Column(name = "anciennete_annees", nullable = false)
    private int ancienneteAnnees;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_journaliste", nullable = false, length = 20)
    private JournalisteStatutQualification statutJournaliste;

    @Enumerated(EnumType.STRING)
    @Column(name = "clause_valide", nullable = false, length = 20)
    private JournalisteStatutClauseValidite clauseValide;

    @Column(name = "indemnite_congediement", nullable = false)
    private BigDecimal indemniteCongediement;

    @Column(name = "commission_arbitrale_requise", nullable = false)
    private boolean commissionArbitraleRequise;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict_global", nullable = false, length = 40)
    private JournalisteStatutVerdict verdictGlobal;

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
