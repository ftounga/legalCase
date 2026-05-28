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
 * SF-219-19 : entity 1:1 par dossier portant l'analyse <i>droit à la
 * déconnexion BE</i> (Loi du 03/10/2022 « Deal pour l'emploi » M.B.
 * 10/11/2022, art. 16 + AR du 19/02/2023 fixant la date d'entrée en
 * vigueur au 01/04/2023 + CCT n° 149 du Conseil national du travail
 * (NAR/CNT) sur le télétravail recommandé).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, ventilation par
 * dimension seuil/instrument/contenu/sensibilisation/organisation) en
 * JSON dans {@code result_data} — pattern uniforme avec les autres
 * outils décisionnels BE (miroir
 * {@link Semaine4JoursBeAnalysis} SF-219-18).</p>
 */
@Entity
@Table(name = "droit_deconnexion_be_analyses")
@Getter
@Setter
public class DroitDeconnexionBeAnalysis {

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
