package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-09 : entity 1:1 par dossier portant l'analyse de la catégorie d'OQTF
 * L. 611-1 CESEDA. Outil single-country FR.
 */
@Entity
@Table(name = "oqtf_categories_analyses")
@Getter
@Setter
public class OqtfCategoriesAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "categorie_l611", nullable = false, length = 10)
    private String categorieL611;

    @Column(name = "date_notification_oqtf", nullable = false)
    private LocalDate dateNotificationOqtf;

    @Column(name = "motif_oqtf", length = 300)
    private String motifOqtf;

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
