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
 * SF-219-09 : entity 1:1 par dossier portant l'analyse de l'obligation
 * d'organiser des élections sociales en Belgique (Loi du 04/12/2007
 * relative aux élections sociales + AR du 25/05/2012 portant exécution
 * + Loi du 19/03/1991 protection des candidats / délégués).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, obligation
 * CE/CPPT, calendrier complet jour Y → X-60, fenêtre protégée
 * candidats, cycle attendu) en JSON dans {@code result_data} — pattern
 * uniforme avec les autres outils décisionnels BE (miroir
 * {@link LicenciementBeCollectifRenaultAnalysis} SF-219-07).</p>
 */
@Entity
@Table(name = "elections_sociales_be_analyses")
@Getter
@Setter
public class ElectionsSocialesBeAnalysis {

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
