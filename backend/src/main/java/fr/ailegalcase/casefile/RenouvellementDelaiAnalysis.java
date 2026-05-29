package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-13 : entity 1:1 par dossier portant l'analyse du délai de dépôt du
 * renouvellement du titre de séjour (2 mois avant expiration, art. R. 433-1
 * CESEDA). Outil single-country FR.
 */
@Entity
@Table(name = "renouvellement_delai_analyses")
@Getter
@Setter
public class RenouvellementDelaiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_expiration_titre", nullable = false)
    private LocalDate dateExpirationTitre;

    @Column(name = "date_depot_dossier")
    private LocalDate dateDepotDossier;

    @Column(name = "type_titre", length = 120)
    private String typeTitre;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private RenouvellementDelaiStatut statut;

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
