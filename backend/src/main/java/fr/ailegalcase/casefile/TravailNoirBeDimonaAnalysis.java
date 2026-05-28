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
 * SF-219-26 : entity 1:1 par dossier portant l'analyse <i>Travail noir
 * BE — DIMONA, requalification et sanctions</i> (Loi-programme du
 * 24/12/2002 art. 167-184 instaurant la DIMONA + AR du 05/11/2002 +
 * Code pénal social art. 181 niveau 4 sanction défaut DIMONA).
 *
 * <p>Snapshot complet (inputs + outputs : verdict DIMONA, cotisations
 * ONSS rétroactives, amende ONSS forfaitaire 3×, sanction pénale art.
 * 181 C. pén. soc. niveau 4 avec majorations) en JSON dans
 * {@code result_data} — pattern uniforme avec les autres outils
 * décisionnels BE (miroir {@link CodePenalSocialBeAnalysis} SF-219-24).</p>
 */
@Entity
@Table(name = "travail_noir_be_dimona_analyses")
@Getter
@Setter
public class TravailNoirBeDimonaAnalysis {

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
