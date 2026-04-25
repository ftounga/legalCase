package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-26-01 : entity 1:1 par dossier portant l'analyse de changement
 * d'état civil (art. 60 / 61-3-1 / 61-5 Cciv).
 * Outil single-country FR (DROIT_FAMILLE).
 */
@Entity
@Table(name = "changement_etat_civil_analyses")
@Getter
@Setter
public class ChangementEtatCivilAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "type_changement", length = 30, nullable = false)
    private String typeChangement;

    @Column(name = "motif_invoque", length = 40, nullable = false)
    private String motifInvoque;

    @Column(name = "preuves_produites", nullable = false, columnDefinition = "TEXT")
    private String preuvesProduites = "[]";

    @Column(name = "majeur_demandeur", nullable = false)
    private boolean majeurDemandeur;

    @Column(name = "consentement_parental", nullable = false)
    private boolean consentementParental;

    @Column(name = "dates_docs_concordants", nullable = false)
    private boolean datesDocsConcordants;

    @Column(name = "deja_change_auparavant", nullable = false)
    private boolean dejaChangeAuparavant;

    @Column(name = "date_naissance_demandeur")
    private LocalDate dateNaissanceDemandeur;

    @Column(name = "departement_declaration", length = 10)
    private String departementDeclaration;

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
