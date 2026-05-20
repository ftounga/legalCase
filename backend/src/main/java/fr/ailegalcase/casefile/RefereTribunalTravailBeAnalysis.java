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
 * SF-207-05 : entité 1:1 par dossier portant la dernière analyse d'éligibilité
 * au référé devant le président du tribunal du travail belge (Code Judiciaire
 * art. 584 ; CJ art. 627 compétence territoriale ; Loi du 3 juillet 1978).
 *
 * <p>Snapshot JSON complet (inputs + résultat) dans {@code result_data} pour
 * restitution UI sans recalcul et survie aux reload (mémoire
 * {@code feedback_decision_tools_all_fields_prefilled}).</p>
 *
 * <p>Outil BE-only — gate {@code workspaceCountry=BELGIQUE} strict (404 côté
 * FR pour préserver l'isolation, pattern SF-207-01 / SF-207-02 / SF-207-04).
 * Substance juridique BE pure (mémoire {@code feedback_belgique_never_forget}) ;
 * le référé prud'homal FR (R.1454-1 CT) est un régime juridiquement distinct
 * géré par {@link ReferePrudhomalAnalysis}.</p>
 */
@Entity
@Table(name = "refere_tribunal_travail_be_analyses")
@Getter
@Setter
public class RefereTribunalTravailBeAnalysis {

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
