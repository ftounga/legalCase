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
 * SF-219-29 : entity 1:1 par dossier portant l'analyse <i>Rente AT/MP
 * vs capitalisation BE</i> (Loi du 10/04/1971 sur les accidents du
 * travail, art. 24 - rente annuelle si taux d'incapacite permanente
 * partielle IPP &gt;= 19% / capitalisation versement unique si IPP &lt; 19% ;
 * Lois coordonnees du 03/06/1970 art. 35 pour les maladies
 * professionnelles MP renvoyant au regime AT pour le calcul ; AR du
 * 21/12/1971 portant execution de certaines dispositions de la Loi du
 * 10/04/1971 - bareme de capitalisation et coefficients d'age ; AR du
 * 10/12/1987 fixant les regles de conversion partielle de la rente en
 * capital ; AR du 24/02/2005 actualisant le bareme).
 *
 * <p>Snapshot complet (inputs + outputs : verdict mode de versement,
 * montant rente annuelle, montant capital de capitalisation, montant
 * conversion possible, base juridique, voies de recours) en JSON dans
 * {@code result_data} — pattern uniforme avec les autres outils
 * decisionnels BE F-219 (miroir
 * {@link MpFedrisReconnaissanceAnalysis} SF-219-28).</p>
 */
@Entity
@Table(name = "at_mp_rente_capital_be_analyses")
@Getter
@Setter
public class AtMpRenteCapitalBeAnalysis {

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
