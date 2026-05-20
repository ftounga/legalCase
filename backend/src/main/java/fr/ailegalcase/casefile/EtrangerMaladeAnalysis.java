package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-01 : entity 1:1 par dossier portant l'analyse étranger malade L. 425-9 CESEDA.
 * Outil single-country FR.
 */
@Entity
@Table(name = "etranger_malade_analyses")
@Getter
@Setter
public class EtrangerMaladeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_depot_dossier_ofii")
    private LocalDate dateDepotDossierOFII;

    @Column(name = "pathologie_principale", nullable = false, length = 500)
    private String pathologiePrincipale;

    @Column(name = "pays_origine", nullable = false, length = 100)
    private String paysOrigine;

    @Column(name = "traitement_disponible_pays_origine", nullable = false)
    private boolean traitementDisponiblePaysOrigine;

    @Column(name = "avis_ofii", length = 20)
    private String avisOFII;

    @Column(name = "date_avis_ofii")
    private LocalDate dateAvisOFII;

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
