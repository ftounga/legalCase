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
 * SF-219-22 : entity 1:1 par dossier portant l'analyse <i>égalité
 * salariale femmes / hommes BE</i> (Loi du 22/04/2012 visant à lutter
 * contre l'écart salarial entre hommes et femmes M.B. 28/08/2012 +
 * AR du 17/08/2013 portant exécution de l'art. 2 + AR du 25/04/2014
 * modifiant les formulaires de rapport d'analyse).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, ventilation par
 * dimension seuil / rapport / contenu / plan d'action / médiateur) en
 * JSON dans {@code result_data} — pattern uniforme avec les autres
 * outils décisionnels BE (miroir
 * {@link DroitDeconnexionBeAnalysis} SF-219-19).</p>
 */
@Entity
@Table(name = "egalite_femmes_hommes_be_analyses")
@Getter
@Setter
public class EgaliteFemmesHommesBeAnalysis {

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
