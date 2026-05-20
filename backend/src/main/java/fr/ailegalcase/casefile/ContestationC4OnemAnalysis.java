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
 * SF-207-03 : entité 1:1 par dossier portant la dernière analyse de
 * contestation d'une décision ONEM (double délai recours administratif
 * Directeur + recours juridictionnel tribunal du travail BE).
 *
 * <p>Snapshot JSON complet (inputs + résultat) dans {@code result_data} pour
 * restitution UI sans recalcul et survie aux reload (mémoire
 * {@code feedback_decision_tools_all_fields_prefilled}).</p>
 *
 * <p>Base juridique :
 * <ul>
 *   <li><b>AR du 25 novembre 1991 art. 144</b> — recours administratif
 *       préalable auprès du Directeur du Bureau du chômage, 1 mois après
 *       notification de la décision ONEM.</li>
 *   <li><b>Code judiciaire art. 580, 2°</b> — recours juridictionnel devant
 *       le tribunal du travail, 3 mois après notification de la décision du
 *       Directeur (ou de l'expiration du délai de réponse).</li>
 *   <li><b>Loi du 3 juillet 1978</b> — contrats de travail (filigrane).</li>
 * </ul>
 */
@Entity
@Table(name = "contestation_c4_onem_analyses")
@Getter
@Setter
public class ContestationC4OnemAnalysis {

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
