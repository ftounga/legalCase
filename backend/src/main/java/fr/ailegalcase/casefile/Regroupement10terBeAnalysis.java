package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-215-03 : entity 1:1 par dossier portant l'analyse d'un regroupement familial
 * art. 10 et 10ter (ressortissant tiers en séjour illimité — carte B ou C) au sens
 * de la Loi du 15/12/1980 + AR du 17/05/2007 + AR du 11/06/2018. Outil
 * <b>single-country BELGIQUE</b>.
 *
 * <p>Pattern miroir de {@link SinglePermitBeAnalysis} (SF-215-01) : snapshot JSON
 * complet (inputs + outputs) dans {@code result_data} pour permettre la restitution
 * UI sans recalcul (pattern F-DT-42).
 */
@Entity
@Table(name = "regroupement_10ter_be_analyses")
@Getter
@Setter
public class Regroupement10terBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "lien_familial", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Regroupement10terBeLienFamilialEnum lienFamilial;

    @Column(name = "type_carte_regroupant", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Regroupement10terBeTypeCarteEnum typeCarteRegroupant;

    @Column(name = "revenus_mensuels_nets_regroupant", nullable = false)
    private Integer revenusMensuelsNetsRegroupant;

    @Column(name = "duree_sejour", nullable = false)
    private Integer dureeSejour;

    @Column(name = "logement_conforme", nullable = false)
    private Boolean logementConforme;

    @Column(name = "assurance_maladie", nullable = false)
    private Boolean assuranceMaladie;

    @Column(name = "menace_ordre_public", nullable = false)
    private Boolean menaceOrdrePublic;

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
