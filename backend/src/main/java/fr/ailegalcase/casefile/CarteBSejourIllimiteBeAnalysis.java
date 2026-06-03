package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-221-02 : entity 1:1 par dossier portant l'analyse de passage carte A → carte B
 * (séjour ILLIMITÉ d'un ressortissant tiers — art. 14 Loi 15/12/1980).
 * Outil <b>single-country BELGIQUE</b>.
 *
 * <p>Pattern miroir de {@link CarteAProrogationBeAnalysis} : snapshot JSON complet
 * (inputs + outputs) dans {@code result_data} pour permettre la restitution UI
 * sans recalcul (pattern F-DT-42).
 */
@Entity
@Table(name = "carte_b_sejour_illimite_be_analyses")
@Getter
@Setter
public class CarteBSejourIllimiteBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_debut_sejour_regulier", nullable = false)
    private LocalDate dateDebutSejourRegulier;

    @Column(name = "sejour_ininterrompu", nullable = false)
    private Boolean sejourIninterrompu;

    @Column(name = "absences_superieures_limites", nullable = false)
    private Boolean absencesSuperieuresLimites;

    @Column(name = "motif_sejour_stable", nullable = false)
    private Boolean motifSejourStable;

    @Column(name = "ordre_public_risque", nullable = false)
    private Boolean ordrePublicRisque;

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
