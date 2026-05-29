package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-215-19 : entity 1:1 par dossier portant l'analyse de la protection temporaire
 * Ukraine (F-IM-34 — directive 2001/55/CE, décision UE 2022/382, Loi 15/12/1980
 * art. 57/29+). Outil <b>single-country BELGIQUE</b>.
 *
 * <p>Pattern miroir de {@link Annexe13quinquiesBeAnalysis} (SF-215-17) : snapshot JSON
 * complet (inputs + outputs) dans {@code result_data} pour permettre la restitution
 * UI sans recalcul (pattern F-DT-42).
 */
@Entity
@Table(name = "protection_temporaire_ukraine_be_analyses")
@Getter
@Setter
public class ProtectionTemporaireUkraineBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_arrivee", nullable = false)
    private LocalDate dateArrivee;

    @Column(name = "nationalite_ukrainienne", nullable = false)
    private Boolean nationaliteUkrainienne;

    @Column(name = "residence_ukraine_avant_24fev2022", nullable = false)
    private Boolean residenceUkraineAvant24Fev2022;

    @Column(name = "apatrides_ukraine", nullable = false)
    private Boolean apatridesUkraine;

    @Column(name = "membre_famille_protege", nullable = false)
    private Boolean membreFamilleProtege;

    @Column(name = "titre_sejour_be", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ProtectionTemporaireUkraineBeTitreSejourEnum titreSejourBE;

    @Column(name = "eligible", nullable = false)
    private Boolean eligible;

    @Column(name = "date_fin_protection", nullable = false)
    private LocalDate dateFinProtection;

    @Column(name = "duree_protection_restante", nullable = false)
    private Long dureeProtectionRestante;

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
