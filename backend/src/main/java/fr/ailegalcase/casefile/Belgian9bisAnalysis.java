package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-14-01 : entity 1:1 par dossier portant l'analyse "9bis humanitaire BE"
 * (art. 9bis Loi 15/12/1980 + AR 17/05/2007). Outil single-country BELGIQUE.
 */
@Entity
@Table(name = "belgian_9bis_analyses")
@Getter
@Setter
public class Belgian9bisAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_entree_belgique", nullable = false)
    private LocalDate dateEntreeBelgique;

    @Column(name = "duree_presence_mois", nullable = false)
    private int dureePresenceMois;

    @Column(name = "circonstances_exceptionnelles", nullable = false)
    private boolean circonstancesExceptionnelles;

    @Column(name = "liens_familiaux_be", nullable = false)
    private boolean liensFamiliauxBe;

    @Column(name = "liens_professionnels", nullable = false)
    private boolean liensProfessionnels;

    @Column(name = "scolarite_enfants_be", nullable = false)
    private boolean scolariteEnfantsBe;

    @Column(name = "menace_ordre_public", nullable = false)
    private boolean menaceOrdrePublic;

    @Column(name = "date_depot_demande")
    private LocalDate dateDepotDemande;

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
