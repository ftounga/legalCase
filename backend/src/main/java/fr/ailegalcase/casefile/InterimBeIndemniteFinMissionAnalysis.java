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
 * SF-219-15 : entity 1:1 par dossier portant l'analyse <i>indemnité
 * de fin de mission intérim BE</i> (Loi 24/07/1987 + CCT n° 322 +
 * CCT n° 322bis + CCT sectorielles + jurisprudence Cass. BE).
 *
 * <p>Calcule les indemnités exigibles à la fin d'une mission
 * intérimaire belge en additionnant pécule de vacances intérim
 * (AR 30/03/1967 art. 19 — 15,38 %), prime de fin d'année
 * sectorielle (CCT propre à la CP de l'utilisateur), indemnité
 * compensatoire pour rupture anticipée sans motif grave (Cass.
 * 04/02/1991, 16/12/2002) et sursalaires heures supplémentaires
 * (Loi 16/03/1971 art. 29). Pas de prime de précarité 10 % style
 * français (Cass. 03/05/2010, 23/06/2003).</p>
 *
 * <p>Snapshot complet (inputs + ventilation détaillée + verdict) en
 * JSON dans {@code result_data} — pattern uniforme avec les autres
 * outils décisionnels BE (miroir {@link InterimBeCct322Analysis}).</p>
 */
@Entity
@Table(name = "interim_be_indemnite_fin_mission_analyses")
@Getter
@Setter
public class InterimBeIndemniteFinMissionAnalysis {

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
