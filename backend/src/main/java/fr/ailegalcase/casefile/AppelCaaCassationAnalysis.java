package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-33 : entity 1:1 par dossier portant l'analyse des délais d'appel CAA /
 * cassation CE en contentieux des étrangers (art. L. 811-1 / R. 811-2 et
 * L. 821-1 / R. 821-1 CJA). Outil single-country FR.
 */
@Entity
@Table(name = "appel_caa_cassation_analyses")
@Getter
@Setter
public class AppelCaaCassationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_jugement_ta", nullable = false)
    private LocalDate dateJugementTA;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_decision_ta", nullable = false, length = 20)
    private AppelCaaCassationTypeDecisionEnum typeDecisionTA;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contentieux", nullable = false, length = 20)
    private AppelCaaCassationTypeContentieuxEnum typeContentieux;

    @Column(name = "delai_special_oqtf", nullable = false)
    private boolean delaiSpecialOQTF;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private AppelCaaCassationStatut statut;

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
