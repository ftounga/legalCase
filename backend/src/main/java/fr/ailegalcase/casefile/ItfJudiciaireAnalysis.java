package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-37 : entity 1:1 par dossier portant l'analyse d'une interdiction du
 * territoire français (ITF) prononcée par un juge pénal comme peine
 * complémentaire (C. pén. 131-30). Outil single-country FR.
 */
@Entity
@Table(name = "itf_judiciaire_analyses")
@Getter
@Setter
public class ItfJudiciaireAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_condamnation", nullable = false)
    private LocalDate dateCondamnation;

    @Column(name = "duree_itf_annees", nullable = false)
    private int dureeItfAnnees;

    @Column(name = "infraction_principale", length = 200)
    private String infractionPrincipale;

    @Column(name = "condamnation_definitive", nullable = false)
    private boolean condamnationDefinitive;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private ItfJudiciaireStatut statut;

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
