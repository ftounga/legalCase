package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-14-01 : entity 1:1 par dossier portant l'analyse d'ordonnance de
 * protection FR (art. 515-9 à 515-13 Cciv + Loi 30/07/2020 BAR). Outil
 * single-country FR.
 */
@Entity
@Table(name = "ordonnance_protection_analyses")
@Getter
@Setter
public class OrdonnanceProtectionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_requete")
    private LocalDate dateRequete;

    /** JSON array (stringifié) des codes de violences alléguées. */
    @Column(name = "violences_alleguees", nullable = false, columnDefinition = "TEXT")
    private String violencesAlleguees = "[]";

    /** JSON array (stringifié) des codes de preuves disponibles. */
    @Column(name = "preuves_violences", nullable = false, columnDefinition = "TEXT")
    private String preuvesViolences = "[]";

    @Column(name = "danger_immediat", nullable = false)
    private boolean dangerImmediat;

    @Column(name = "presence_enfants", nullable = false)
    private boolean presenceEnfants;

    /** JSON array (stringifié) des âges d'enfants ; nullable. */
    @Column(name = "age_enfants", columnDefinition = "TEXT")
    private String ageEnfants;

    @Column(name = "logement_commun", nullable = false)
    private boolean logementCommun;

    @Column(name = "victime_financierement_dependante", nullable = false)
    private boolean victimeFinanciairementDependante;

    @Column(name = "demandeur_deja_protege", nullable = false)
    private boolean demandeurDejaProtege;

    /** SF-222-05 : DEC envisagé par l'avocat (suivi électronique du contact). */
    @Column(name = "dec_envisage", nullable = false)
    private boolean decEnvisage;

    /** JSON array (stringifié) des codes de mesures demandées. */
    @Column(name = "demande_mesures", nullable = false, columnDefinition = "TEXT")
    private String demandeMesures = "[]";

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
