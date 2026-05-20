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
 * SF-207-08 : entité 1:1 par dossier portant la dernière analyse de conformité
 * de l'<b>outplacement obligatoire 45+ ans</b> (BE). Sources juridiques :
 * <ul>
 *   <li>CCT n°82 (CCT n°82 bis pour temps partiel) — outplacement obligatoire
 *       pour salariés ≥ 45 ans, ≥ 1 an d'ancienneté, licenciés (hors faute
 *       grave / hors démission) ;</li>
 *   <li>Loi du 5 septembre 2001 art. 13 — base légale, sanction administrative ;</li>
 *   <li>AR du 30 mai 2018 — sanction administrative employeur 1 800 € en cas de
 *       non-offre / non-conformité ;</li>
 *   <li>AR du 25 novembre 1991 art. 154 — sanction salarié 4 à 52 semaines
 *       d'exclusion ONEM en cas de refus d'offre conforme.</li>
 * </ul>
 *
 * <p>Snapshot JSON complet (inputs + résultat) dans {@code result_data} pour
 * restitution UI sans recalcul et survie aux reload (mémoire
 * {@code feedback_decision_tools_all_fields_prefilled}).</p>
 *
 * <p>Outil BE-only — gate {@code workspaceCountry=BELGIQUE} strict (404 côté
 * FR pour préserver l'isolation, pattern SF-207-01..07). Substance juridique
 * BE pure (mémoire {@code feedback_belgique_never_forget}) ; aucun
 * équivalent FR direct (les régimes français d'aide au reclassement relèvent
 * de dispositifs distincts — CSP, congé reclassement).</p>
 */
@Entity
@Table(name = "outplacement_be_analyses")
@Getter
@Setter
public class OutplacementBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    /** Snapshot JSON sérialisé (inputs + outputs) — source de vérité pour le GET. */
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
