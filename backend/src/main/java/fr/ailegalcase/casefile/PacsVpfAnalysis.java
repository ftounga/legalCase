package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-220-04 : entity 1:1 par dossier portant l'analyse VPF au titre d'un PACS
 * (CESEDA L.423-23, F-IM-50-pacs-vpf-fr). Outil single-country FR.
 */
@Entity
@Table(name = "pacs_vpf_analyses")
@Getter
@Setter
public class PacsVpfAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "pacs_conclu", nullable = false)
    private boolean pacsConclu;

    @Column(name = "date_pacs")
    private LocalDate datePacs;

    @Column(name = "partenaire_statut", length = 30)
    private String partenaireStatut;

    @Column(name = "duree_vie_commune_mois")
    private Integer dureeVieCommuneMois;

    @Column(name = "intensite_communaute_vie", length = 20)
    private String intensiteCommunauteVie;

    @Column(name = "autres_liens_prives_familiaux", nullable = false)
    private boolean autresLiensPrivesFamiliaux;

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
