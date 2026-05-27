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
 * SF-219-06 : entity 1:1 par dossier portant l'analyse du licenciement
 * BE en cas de fermeture d'entreprise (Loi 26/06/2002, AR 23/03/2007,
 * Fonds Fermeture Entreprises FFE).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, indemnité de
 * fermeture calculée, créances FFE reprises, raison) en JSON dans
 * {@code result_data} — pattern uniforme avec les autres outils
 * décisionnels BE (miroir {@link CumulRccAllocationsAnalysis}
 * SF-219-04).</p>
 */
@Entity
@Table(name = "licenciement_be_fermeture_entreprise_analyses")
@Getter
@Setter
public class LicenciementBeFermetureEntrepriseAnalysis {

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
