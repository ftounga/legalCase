package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-214-03 : entity 1:1 par dossier portant l'analyse d'éligibilité au
 * regroupement familial L. 434-1+ CESEDA. Outil single-country FR.
 */
@Entity
@Table(name = "regroupement_familial_analyses")
@Getter
@Setter
public class RegroupementFamilialAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "duree_sejour_regulier_mois", nullable = false)
    private int dureeSejourRegulierMois;

    @Column(name = "ressources_mensuelles_nettes", nullable = false)
    private double ressourcesMensuellesNettes;

    @Column(name = "taille_logement_m2", nullable = false)
    private int tailleLogementM2;

    @Column(name = "nombre_personnes_foyer", nullable = false)
    private int nombrePersonnesFoyer;

    @Column(name = "type_regroupement", nullable = false, length = 20)
    private String typeRegroupement;

    @Column(name = "membres_famille_a_regrouper", nullable = false)
    private int membresFamilleARegrouper;

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
