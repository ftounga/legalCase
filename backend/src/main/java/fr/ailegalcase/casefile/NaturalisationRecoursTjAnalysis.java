package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-29 : entity 1:1 par dossier portant l'analyse du délai de recours
 * devant le Tribunal judiciaire contre un refus de déclaration de nationalité
 * française (Cciv 26-3). Outil single-country FR.
 */
@Entity
@Table(name = "naturalisation_recours_tj_analyses")
@Getter
@Setter
public class NaturalisationRecoursTjAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "voie_naturalisation", nullable = false, length = 30)
    private NaturalisationRecoursTjVoieEnum voieNaturalisation;

    @Column(name = "date_refus_declaration", nullable = false)
    private LocalDate dateRefusDeclaration;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_refus", nullable = false, length = 30)
    private NaturalisationRecoursTjTypeRefusEnum typeRefus;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private NaturalisationRecoursTjStatut statut;

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
