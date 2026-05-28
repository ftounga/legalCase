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
 * SF-219-08 : entity 1:1 par dossier portant l'analyse <i>transfert
 * d'entreprise CCT n° 32bis du 07/06/1985</i> (transfert conventionnel
 * BE — Loi du 17/03/1965 ; Directive 2001/23/CE).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, droits maintenus,
 * obligations procédurales) en JSON dans {@code result_data} — pattern
 * uniforme avec les autres outils décisionnels BE (miroir {@link
 * LicenciementBeFermetureEntrepriseAnalysis} SF-219-06).</p>
 */
@Entity
@Table(name = "transfert_entreprise_cct_32bis_analyses")
@Getter
@Setter
public class TransfertEntrepriseCct32bisAnalysis {

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
