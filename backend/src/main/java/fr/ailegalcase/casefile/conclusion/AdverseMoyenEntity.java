package fr.ailegalcase.casefile.conclusion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-261 / SF-261-05 — moyen de la partie adverse PERSISTÉ (extrait des écritures
 * adverses, SF-261-02) avec un identifiant <strong>stable</strong> par dossier.
 *
 * <p>Auparavant les moyens étaient ré-extraits par LLM à chaque génération (record
 * éphémère {@link AdverseMoyen}, sans id). Cette entité fige le set extrait :
 * {@code AdverseMoyenPersistenceService} lit le persisté plutôt que de ré-extraire,
 * et la curation des moyens (F-288 vague 3) référence chaque moyen par son
 * {@link #id} stable.</p>
 *
 * <p>{@code fondements} et {@code piecesInvoquees} (listes de chaînes de
 * {@link AdverseMoyen}) sont sérialisés en tableaux JSON stockés en texte ; la
 * (dé)sérialisation est portée par le service (l'entité reste sans logique métier,
 * coding-rules). {@code ordre} fige la position du moyen dans le set (0..N) pour une
 * lecture déterministe. Isolation workspace via {@code case_file_id}.</p>
 */
@Entity
@Table(name = "adverse_moyen")
@Getter
@Setter
public class AdverseMoyenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_file_id", nullable = false)
    private UUID caseFileId;

    @Column(name = "these", nullable = false)
    private String these;

    /** Fondements juridiques invoqués, sérialisés en tableau JSON ({@code []} si aucun). */
    @Column(name = "fondements")
    private String fondements;

    /** Pièces invoquées par l'adversaire, sérialisées en tableau JSON ({@code []} si aucune). */
    @Column(name = "pieces_invoquees")
    private String piecesInvoquees;

    @Column(name = "ordre", nullable = false)
    private int ordre;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
