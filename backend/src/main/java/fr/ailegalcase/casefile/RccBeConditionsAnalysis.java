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
 * SF-207-06 : entité 1:1 par dossier portant la dernière analyse
 * d'éligibilité au Régime de Chômage avec Complément d'entreprise (RCC,
 * ex-prépension belge). Sources juridiques :
 * <ul>
 *   <li>CCT n°17 (régime général + variante longue carrière) ;</li>
 *   <li>CCT n°17/13 (régime métiers lourds) ;</li>
 *   <li>AR du 03/05/2007 art. 3 et 8 (entreprise en difficulté).</li>
 * </ul>
 *
 * <p>Snapshot JSON complet (inputs + résultat) dans {@code result_data} pour
 * restitution UI sans recalcul et survie aux reload (mémoire
 * {@code feedback_decision_tools_all_fields_prefilled}).</p>
 *
 * <p>Outil BE-only — gate {@code workspaceCountry=BELGIQUE} strict (404 côté
 * FR pour préserver l'isolation, pattern SF-207-01..05). Substance juridique
 * BE pure (mémoire {@code feedback_belgique_never_forget}) ; aucun
 * équivalent FR direct.</p>
 */
@Entity
@Table(name = "rcc_be_conditions_analyses")
@Getter
@Setter
public class RccBeConditionsAnalysis {

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
