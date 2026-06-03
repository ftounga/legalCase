package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-51 : entity 1:1 par dossier portant l'analyse de qualification du temps
 * de trajet / déplacement professionnel (art. L.3121-4 CT ; CJUE C-266/14,
 * F-DT-81). Outil single-country FR.
 */
@Entity
@Table(name = "temps_trajet_deplacement_analyses")
@Getter
@Setter
public class TempsTrajetDeplacementAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualification", nullable = false, length = 30)
    private TempsTrajetQualification qualification;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_trajet", nullable = false, length = 40)
    private TypeTrajet typeTrajet;

    @Column(name = "temps_trajet_quotidien_minutes", nullable = false)
    private int tempsTrajetQuotidienMinutes;

    @Column(name = "temps_trajet_normal_minutes", nullable = false)
    private int tempsTrajetNormalMinutes;

    @Column(name = "contrepartie_prevue_accord", nullable = false)
    private boolean contrepartiePrevueAccord;

    @Column(name = "contrepartie_due", nullable = false)
    private boolean contrepartieDue;

    @Column(name = "depassement_minutes", nullable = false)
    private int depassementMinutes;

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
