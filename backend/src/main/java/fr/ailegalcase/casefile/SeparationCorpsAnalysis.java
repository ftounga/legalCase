package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-21-01 : entity 1:1 par dossier portant l'analyse "Séparation de corps
 * + conversion en divorce" (art. 296 et s. + 306 Code civil + Loi 26/05/2004).
 * Outil single-country FRANCE + DROIT_FAMILLE.
 */
@Entity
@Table(name = "separation_corps_analyses")
@Getter
@Setter
public class SeparationCorpsAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "mode_procedure", nullable = false, length = 30)
    private String modeProcedure;

    @Column(name = "date_jugement_separation_corps")
    private LocalDate dateJugementSeparationCorps;

    @Column(name = "date_requete_conversion")
    private LocalDate dateRequeteConversion;

    @Column(name = "duree_separation_annees", nullable = false)
    private int dureeSeparationAnnees;

    @Column(name = "consentement_mutuel_conversion", nullable = false)
    private boolean consentementMutuelConversion;

    @Column(name = "patrimoine_commun", nullable = false)
    private boolean patrimoineCommun;

    @Column(name = "enfants_mineurs", nullable = false)
    private int enfantsMineurs;

    @Column(name = "demande_reconciliation_formulee", nullable = false)
    private boolean demandeReconciliationFormulee;

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
