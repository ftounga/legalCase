package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-221-04 : entity 1:1 par dossier portant l'analyse de détention en centre fermé +
 * requête de mise en liberté (Loi 15/12/1980 art. 7 al. 3 / 27 / 29 / 74/5 ;
 * AR 02/08/2002 ; chambre du conseil art. 71 et s.). Outil <b>single-country BELGIQUE</b>.
 *
 * <p>Pattern miroir de {@link ResidenceLongueDureeUeBeAnalysis} : snapshot JSON complet
 * (inputs + outputs) dans {@code result_data} pour permettre la restitution UI sans
 * recalcul (pattern F-DT-42).
 */
@Entity
@Table(name = "detention_centre_ferme_be_analyses")
@Getter
@Setter
public class DetentionCentreFermeBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_debut_detention", nullable = false)
    private LocalDate dateDebutDetention;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_legale_detention", nullable = false, length = 20)
    private DetentionBaseLegale baseLegaleDetention;

    @Column(name = "prolongation_notifiee", nullable = false)
    private Boolean prolongationNotifiee;

    @Column(name = "date_prolongation")
    private LocalDate dateProlongation;

    @Column(name = "requete_mise_en_liberte_deposee", nullable = false)
    private Boolean requeteMiseEnLiberteDeposee;

    @Column(name = "date_notification_decision_detention")
    private LocalDate dateNotificationDecisionDetention;

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
