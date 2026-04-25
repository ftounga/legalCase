package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-08-07 : entity 1:1 par dossier portant l'analyse des référés administratifs combinés FR
 * (L.521-1 + L.521-2 CJA). Outil single-country FR.
 */
@Entity
@Table(name = "referes_admin_analyses")
@Getter
@Setter
public class ReferesAdminAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "type_refere", nullable = false, length = 20)
    private String typeRefere;

    @Column(name = "decision_contestee", nullable = false, length = 40)
    private String decisionContestee;

    @Column(name = "date_notification_decision", nullable = false)
    private LocalDate dateNotificationDecision;

    @Column(name = "urgence_caracterisee", nullable = false)
    private boolean urgenceCaracterisee;

    @Column(name = "atteinte_liberte_fondamentale", nullable = false)
    private boolean atteinteLiberteFondamentale;

    @Column(name = "doutes_serieux_legalite", nullable = false)
    private boolean doutesSerieuxLegalite;

    @Column(name = "demandeur_deja_prived", nullable = false)
    private boolean demandeurDejaPrived;

    @Column(name = "preuves_urgence", columnDefinition = "TEXT")
    private String preuvesUrgenceJson;

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
