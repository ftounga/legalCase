package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-215-09 : entity 1:1 par dossier portant l'analyse d'une déclaration de
 * nationalité belge par mariage avec un(e) Belge — art. 16 Code de la nationalité belge
 * (loi 28/06/1984 consolidée, loi modificative 04/12/2012).
 *
 * <p>Outil <b>single-country BELGIQUE</b>. Pattern miroir de
 * {@link Naturalisation12bisBeAnalysis} (SF-215-07) — snapshot JSON complet (inputs +
 * outputs) dans {@code result_data} pour permettre la restitution UI sans recalcul
 * (pattern F-DT-42).
 */
@Entity
@Table(name = "naturalisation_conjoint_belge_be_analyses")
@Getter
@Setter
public class NaturalisationConjointBelgeBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_marriage", nullable = false)
    private LocalDate dateMarriage;

    @Column(name = "cohabitation_legale", nullable = false)
    private Boolean cohabitationLegale;

    @Column(name = "duree_cohabitation_mois", nullable = false)
    private Integer dureeCohabitationMois;

    @Column(name = "niveau_langue", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NaturalisationBeNiveauLangueEnum niveauLangue;

    @Column(name = "preuve_integration", nullable = false)
    private Boolean preuveIntegration;

    @Column(name = "menace_ordre_public", nullable = false)
    private Boolean menaceOrdrePublic;

    @Column(name = "condamnation_penale", nullable = false)
    private Boolean condamnationPenale;

    @Column(name = "eligible", nullable = false)
    private Boolean eligible;

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
