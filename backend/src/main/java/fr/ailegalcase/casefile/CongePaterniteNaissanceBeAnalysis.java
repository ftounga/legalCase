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
 * SF-219-31 : entity 1:1 par dossier portant l'analyse <i>Congé
 * paternité / naissance BE</i> (Loi du 03/07/1978 art. 30 § 2 + Loi du
 * 12/08/2000 + Loi du 07/04/2023 réforme Deal pour l'emploi).
 *
 * <p>Snapshot complet (inputs + outputs : verdict d'éligibilité, durée
 * applicable, dates calculées des échéances légales, protection
 * licenciement 5 mois) en JSON dans {@code result_data} — pattern
 * uniforme avec les autres outils décisionnels BE.</p>
 */
@Entity
@Table(name = "conge_paternite_naissance_be_analyses")
@Getter
@Setter
public class CongePaterniteNaissanceBeAnalysis {

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
