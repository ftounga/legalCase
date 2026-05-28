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
 * SF-219-20 : entity 1:1 par dossier portant l'analyse <i>pécule de
 * vacances BE</i> (Lois coordonnées du 28/06/1971 relatives aux
 * vacances annuelles des travailleurs salariés et AR du 30/03/1967
 * déterminant les modalités générales d'exécution).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, montants par type
 * de pécule simple / double / départ, base de calcul, fractions
 * légales appliquées) en JSON dans {@code result_data} — pattern
 * uniforme avec les autres outils décisionnels BE (miroir
 * {@link Semaine4JoursBeAnalysis} SF-219-18,
 * {@link ClauseEcolageBeAnalysis} SF-219-17).</p>
 */
@Entity
@Table(name = "pecule_vacances_be_analyses")
@Getter
@Setter
public class PeculeVacancesBeAnalysis {

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
