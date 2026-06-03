package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-220-02 : entity 1:1 par dossier portant l'analyse de portée territoriale d'un
 * titre délivré à Mayotte (Ord. 2014-464, CESEDA L.832-1 et s.). Outil
 * single-country FR (F-IM-48-regime-mayotte-fr).
 */
@Entity
@Table(name = "regime_mayotte_analyses")
@Getter
@Setter
public class RegimeMayotteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "titre_delivre_a_mayotte", nullable = false)
    private boolean titreDelivreAMayotte;

    @Column(name = "type_titre", nullable = false, length = 20)
    private String typeTitre;

    @Column(name = "projet_deplacement_metropole", nullable = false)
    private boolean projetDeplacementMetropole;

    @Column(name = "date_delivrance")
    private LocalDate dateDelivrance;

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
