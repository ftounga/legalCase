package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-FA-23-01 : entity 1:1 par dossier portant l'analyse d'ordonnance sur requête
 * (mesures urgentes familiales — art. 493 CPC FR / 1025 CJ BE).
 */
@Entity
@Table(name = "ordonnance_requete_analyses")
@Getter
@Setter
public class OrdonnanceRequeteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_requete", nullable = false, length = 40)
    private OrdonnanceRequeteCalculator.MotifRequete motifRequete;

    @Column(name = "urgence_justifiee", nullable = false)
    private boolean urgenceJustifiee;

    @Column(name = "derogation_contradictoire_justifiee", nullable = false)
    private boolean derogationContradictoireJustifiee;

    @Column(name = "piece_justificative_fournie", nullable = false)
    private boolean pieceJustificativeFournie;

    @Column(name = "presence_enfants", nullable = false)
    private boolean presenceEnfants;

    @Column(name = "commentaire_urgence", columnDefinition = "TEXT")
    private String commentaireUrgence;

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
