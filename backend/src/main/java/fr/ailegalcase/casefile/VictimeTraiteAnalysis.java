package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-21 : entity 1:1 par dossier portant l'analyse d'éligibilité au titre
 * victime de la traite des êtres humains L. 425-1 CESEDA. Outil single-country FR.
 */
@Entity
@Table(name = "victime_traite_analyses")
@Getter
@Setter
public class VictimeTraiteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "plainte_deposee", nullable = false)
    private boolean plainteDeposee;

    @Column(name = "collaboration_ocrteh", nullable = false)
    private boolean collaborationOcrteh;

    @Column(name = "date_plainte")
    private LocalDate datePlainte;

    @Column(name = "titre_actuel", length = 120)
    private String titreActuel;

    @Column(name = "presence_autorite_refugie_detectee", nullable = false)
    private boolean presenceAutoriteRefugieDetectee;

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
