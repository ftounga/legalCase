package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-208-04 : entity 1:1 par dossier portant l'analyse d'éligibilité au titre
 * vie privée et familiale "victime de violences" (CESEDA L.425-6+).
 * Outil single-country FR.
 */
@Entity
@Table(name = "victime_violences_l4256_analyses")
@Getter
@Setter
public class VictimeViolencesL4256Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_ordonnance_protection", nullable = false)
    private LocalDate dateOrdonnanceProtection;

    @Column(name = "juridiction", nullable = false, length = 80)
    private String juridiction;

    @Column(name = "duree_protection_mois", nullable = false)
    private int dureeProtectionMois = 6;

    @Column(name = "date_expiration_protection")
    private LocalDate dateExpirationProtection;

    @Column(name = "enfants_a_charge", nullable = false)
    private int enfantsAcharge;

    @Column(name = "nationalite", length = 80)
    private String nationalite;

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
