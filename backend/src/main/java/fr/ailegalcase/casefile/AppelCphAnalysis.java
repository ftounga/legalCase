package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-01 : entity 1:1 par dossier portant l'analyse de l'appel d'un jugement
 * CPH devant la chambre sociale de la Cour d'appel (art. 538 CPC ; R. 1461-1
 * CPC). Outil single-country FR.
 */
@Entity
@Table(name = "appel_cph_analyses")
@Getter
@Setter
public class AppelCphAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_notification_jugement", nullable = false)
    private LocalDate dateNotificationJugement;

    @Enumerated(EnumType.STRING)
    @Column(name = "partie_appelante", nullable = false, length = 20)
    private AppelCphPartieAppelante partieAppelante;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_notification", nullable = false, length = 20)
    private AppelCphModeNotification modeNotification;

    @Enumerated(EnumType.STRING)
    @Column(name = "representation_constituee", nullable = false, length = 30)
    private AppelCphRepresentation representationConstituee;

    @Column(name = "jugement_en_dernier_ressort", nullable = false)
    private boolean jugementEnDernierRessort;

    @Column(name = "date_limite_appel", nullable = false)
    private LocalDate dateLimiteAppel;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 20)
    private AppelCphVerdict verdict;

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
