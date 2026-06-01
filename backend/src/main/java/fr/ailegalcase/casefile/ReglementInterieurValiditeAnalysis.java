package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-35 : entity 1:1 par dossier portant l'analyse de validité d'un règlement
 * intérieur (art. L.1311-1 à L.1322-4, L.1321-1 et s. CT, F-DT-100). Outil
 * single-country FR.
 */
@Entity
@Table(name = "reglement_interieur_validite_analyses")
@Getter
@Setter
public class ReglementInterieurValiditeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "effectif", nullable = false)
    private int effectif;

    @Column(name = "reglement_existe", nullable = false)
    private boolean reglementExiste;

    @Column(name = "items_obligatoires_manquants", nullable = false)
    private int itemsObligatoiresManquants;

    @Column(name = "clauses_interdites_presentes", nullable = false)
    private int clausesInterditesPresentes;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private ReglementInterieurValiditeStatut statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "opposabilite", nullable = false, length = 20)
    private ReglementInterieurOpposabilite opposabilite;

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
