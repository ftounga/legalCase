package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * F-283 / SF-283-01 — une transition de phase procédurale d'un dossier :
 * le dossier est entré dans la phase {@code phase} à la date {@code enteredAt}.
 * La suite ordonnée de ces transitions forme la progression datée du dossier
 * (Saisine → Conciliation → Fond → Appel → Cassation…). Isolation workspace
 * via {@link CaseFile}. Distinct de {@link ContradictoireRound} (échanges) et
 * de {@code CaseFile.procedureStage} (libellé courant statique, F-243).
 */
@Entity
@Table(name = "case_phases")
@Getter
@Setter
public class CasePhase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CasePhaseType phase;

    @Column(length = 200)
    private String label;

    /** Date d'entrée dans la phase. */
    @Column(name = "entered_at", nullable = false)
    private LocalDate enteredAt;

    @Column(length = 2000)
    private String note;

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
