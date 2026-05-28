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
 * SF-219-16 : entity 1:1 par dossier portant l'analyse <i>télétravail BE
 * — CCT n° 85 (structurel) / CCT n° 149 (occasionnel)</i> (CCT n° 85 du
 * 09/11/2005 du CNT — télétravail régulier et structurel ; CCT n° 149
 * du 26/01/2021 du CNT — télétravail recommandé ou obligatoire en
 * raison de la crise du coronavirus, prolongée par les partenaires
 * sociaux en cadre de référence du télétravail occasionnel ; Loi du
 * 03/07/1978 sur les contrats de travail, art. 17 ; Loi du 04/08/1996
 * relative au bien-être des travailleurs ; AR Code du bien-être au
 * travail Livre VIII / Titre 1).
 *
 * <p>Snapshot complet (inputs + outputs : verdict, ventilation par
 * dimension type/convention/équipement/indemnité/déconnexion) en
 * JSON dans {@code result_data} — pattern uniforme avec les autres
 * outils décisionnels BE (miroir
 * {@link InterimBeCct322Analysis} SF-219-14).</p>
 */
@Entity
@Table(name = "teletravail_be_cct_85_149_analyses")
@Getter
@Setter
public class TeletravailBeCct85149Analysis {

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
