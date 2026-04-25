package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-30-01 : entity 1:1 par dossier portant l'analyse de régularité de la procédure
 * de licenciement d'un salarié protégé (FR).
 */
@Entity
@Table(name = "protection_rp_analyses")
@Getter
@Setter
public class ProtectionRpAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_protege", nullable = false, length = 50)
    private ProtectionRpCalculator.StatutProtege statutProtege;

    @Column(name = "date_expiration_mandat", nullable = false)
    private LocalDate dateExpirationMandat;

    @Column(name = "date_presumee_rupture", nullable = false)
    private LocalDate datePresumeeRupture;

    @Enumerated(EnumType.STRING)
    @Column(name = "procedure_suivie", nullable = false, length = 30)
    private ProtectionRpCalculator.ProcedureSuivie procedureSuivie;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_licenciement", nullable = false, length = 30)
    private ProtectionRpCalculator.MotifLicenciement motifLicenciement;

    @Column(name = "salaire_mensuel_brut_eur", nullable = false)
    private double salaireMensuelBrutEur;

    @Column(name = "country", nullable = false, length = 20)
    private String country;

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
