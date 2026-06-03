package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-43 : entity 1:1 par dossier portant l'analyse du congé pour évènement
 * familial (art. L.3142-1 à L.3142-5 CT, F-DT-76). Outil single-country FR.
 */
@Entity
@Table(name = "conges_evenements_familiaux_analyses")
@Getter
@Setter
public class CongesEvenementsFamiliauxAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_evenement", nullable = false, length = 40)
    private CongesEvenementsFamiliauxTypeEvenement typeEvenement;

    @Column(name = "convention_plus_favorable", nullable = false)
    private boolean conventionPlusFavorable;

    @Column(name = "duree_conventionnelle_jours")
    private Integer dureeConventionnelleJours;

    @Column(name = "duree_legale_jours", nullable = false)
    private int dureeLegaleJours;

    @Column(name = "duree_applicable_jours", nullable = false)
    private int dureeApplicableJours;

    @Enumerated(EnumType.STRING)
    @Column(name = "base", nullable = false, length = 20)
    private CongesEvenementsFamiliauxBase base;

    @Column(name = "maintien_salaire", nullable = false)
    private boolean maintienSalaire;

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
