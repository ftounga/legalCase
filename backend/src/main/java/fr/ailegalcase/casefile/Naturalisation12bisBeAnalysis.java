package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-215-07 : entity 1:1 par dossier portant l'analyse d'une déclaration de
 * nationalité belge au sens de l'art. 12bis CNB (loi du 28/06/1984 consolidée).
 * Outil <b>single-country BELGIQUE</b>.
 *
 * <p>Pattern miroir de {@link Regroupement10bisBeAnalysis} (SF-215-05) :
 * snapshot JSON complet (inputs + outputs) dans {@code result_data} pour
 * permettre la restitution UI sans recalcul (pattern F-DT-42).
 */
@Entity
@Table(name = "naturalisation_12bis_be_analyses")
@Getter
@Setter
public class Naturalisation12bisBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "duree_sejour", nullable = false)
    private Integer dureeSejour;

    @Column(name = "type_sejour", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NaturalisationBeTypeSejourEnum typeSejour;

    @Column(name = "niveau_langue", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NaturalisationBeNiveauLangueEnum niveauLangue;

    @Column(name = "preuve_integration", nullable = false)
    private Boolean preuveIntegration;

    @Column(name = "preuve_emploi", nullable = false)
    private Boolean preuveEmploi;

    @Column(name = "menace_ordre_public", nullable = false)
    private Boolean menaceOrdrePublic;

    @Column(name = "condamnation_penale", nullable = false)
    private Boolean condamnationPenale;

    @Column(name = "voie_eligible", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Naturalisation12bisBeVoieEligible voieEligible;

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
