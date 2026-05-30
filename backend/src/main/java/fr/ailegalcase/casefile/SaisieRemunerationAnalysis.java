package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-07 : entity 1:1 par dossier portant l'analyse de la saisie sur
 * rémunération (quotité saisissable — art. R. 3252-2 et s. Code du travail).
 * Outil single-country FR.
 */
@Entity
@Table(name = "saisie_remuneration_analyses")
@Getter
@Setter
public class SaisieRemunerationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "remuneration_nette_mensuelle", nullable = false)
    private double remunerationNetteMensuelle;

    @Column(name = "nombre_personnes_a_charge", nullable = false)
    private int nombrePersonnesACharge;

    @Column(name = "creance_totale", nullable = false)
    private double creanceTotale;

    @Column(name = "creance_alimentaire", nullable = false)
    private boolean creanceAlimentaire;

    @Column(name = "quotite_saisissable_mensuelle", nullable = false)
    private double quotiteSaisissableMensuelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 30)
    private SaisieRemunerationVerdict verdict;

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
