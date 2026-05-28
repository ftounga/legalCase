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
 * SF-219-24 : entity 1:1 par dossier portant l'analyse <i>Code pénal
 * social BE — qualification d'infraction + niveau de sanction 1 à 4</i>
 * (Loi du 06/06/2010 introduisant le Code pénal social, M.B.
 * 01/07/2010, e.e.v. 01/07/2011).
 *
 * <p>Snapshot complet (inputs + outputs : verdict niveau de sanction,
 * ventilation par dimension qualification / sanction / récidive /
 * personnes responsables) en JSON dans {@code result_data} — pattern
 * uniforme avec les autres outils décisionnels BE (miroir
 * {@link EgaliteFemmesHommesBeAnalysis} SF-219-22).</p>
 */
@Entity
@Table(name = "code_penal_social_be_analyses")
@Getter
@Setter
public class CodePenalSocialBeAnalysis {

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
