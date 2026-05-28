package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-219-32 : entity 1:1 par dossier portant l'analyse <i>Interruption
 * de carrière pour congé parental BE</i> (Loi de redressement du
 * 22/01/1985 art. 99 à 107quater + AR du 29/10/1997 + CCT n° 64).
 *
 * <p>Snapshot complet (inputs + outputs : verdict d'éligibilité, durée
 * effective, allocations ONEM mensuelle et totale, solde restant,
 * fenêtre de protection contre licenciement) en JSON dans
 * {@code result_data} — pattern uniforme avec les autres outils
 * décisionnels BE.</p>
 */
@Entity
@Table(name = "interruption_carriere_soins_parental_analyses")
@Getter
@Setter
public class InterruptionCarriereSoinsParentalAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

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
