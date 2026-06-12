package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * F-282 / SF-282-01 — un round du cycle contradictoire d'un dossier : le jeu de
 * conclusions déposé (ou reçu) par une partie à une date donnée, avec l'échéance
 * de réponse éventuelle. Isolation workspace via {@link CaseFile}.
 */
@Entity
@Table(name = "contradictoire_rounds")
@Getter
@Setter
public class ContradictoireRound {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    /** Numéro de round, incrémental par dossier (1 = saisine). */
    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ContradictoireParty party;

    @Column(length = 200)
    private String label;

    /** Date de dépôt (nous) ou de réception (adverse) du jeu de conclusions. */
    @Column(name = "dated_at", nullable = false)
    private LocalDate datedAt;

    /** Échéance pour répondre à ce round (le cas échéant). */
    @Column(name = "response_due_at")
    private LocalDate responseDueAt;

    /** Document source (jeu adverse uploadé), optionnel. */
    @Column(name = "source_document_id")
    private UUID sourceDocumentId;

    /** Version de conclusions source (notre jeu), optionnel. */
    @Column(name = "source_conclusion_id")
    private UUID sourceConclusionId;

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
