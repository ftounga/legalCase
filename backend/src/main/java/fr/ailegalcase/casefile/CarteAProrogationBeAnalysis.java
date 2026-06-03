package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-221-01 : entity 1:1 par dossier portant l'analyse de prorogation de la
 * carte A (séjour temporaire BE — art. 13 Loi 15/12/1980 + art. 33 AR 08/10/1981).
 * Outil <b>single-country BELGIQUE</b>.
 *
 * <p>Pattern miroir de {@link Regroupement10terBeAnalysis} : snapshot JSON complet
 * (inputs + outputs) dans {@code result_data} pour permettre la restitution UI
 * sans recalcul (pattern F-DT-42).
 */
@Entity
@Table(name = "carte_a_prorogation_be_analyses")
@Getter
@Setter
public class CarteAProrogationBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_expiration_carte_a", nullable = false)
    private LocalDate dateExpirationCarteA;

    @Column(name = "motif_sejour_persiste", nullable = false)
    private Boolean motifSejourPersiste;

    @Column(name = "conditions_initiales_toujours_reunies", nullable = false)
    private Boolean conditionsInitialesToujoursReunies;

    @Column(name = "demande_deposee", nullable = false)
    private Boolean demandeDeposee;

    @Column(name = "date_demande")
    private LocalDate dateDemande;

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
