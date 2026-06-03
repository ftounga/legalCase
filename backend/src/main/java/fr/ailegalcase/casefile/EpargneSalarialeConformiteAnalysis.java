package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-41 : entity 1:1 par dossier portant l'analyse de conformité aux
 * obligations d'épargne salariale (intéressement / participation / dispositif de
 * partage de la valeur — art. L.3311-1 et s., L.3321-1 et s., L.3322-2 CT + loi
 * n° 2023-1107 du 29/11/2023, F-DT-53). Outil single-country FR.
 */
@Entity
@Table(name = "epargne_salariale_conformite_analyses")
@Getter
@Setter
public class EpargneSalarialeConformiteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "effectif", nullable = false)
    private int effectif;

    @Column(name = "accord_participation_present", nullable = false)
    private boolean accordParticipationPresent;

    @Column(name = "accord_interessement_present", nullable = false)
    private boolean accordInteressementPresent;

    @Column(name = "benefice_net_fiscal_positif_3ans", nullable = false)
    private boolean beneficeNetFiscalPositif3Ans;

    @Column(name = "entreprise_11_a_49", nullable = false)
    private boolean entreprise11a49;

    @Column(name = "obligations_applicables", nullable = false)
    private int obligationsApplicables;

    @Column(name = "obligations_non_remplies", nullable = false)
    private int obligationsNonRemplies;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private EpargneSalarialeConformiteStatut statut;

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
