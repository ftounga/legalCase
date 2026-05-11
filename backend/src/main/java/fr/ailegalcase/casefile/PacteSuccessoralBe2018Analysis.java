package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-211-04 : entity 1:1 par dossier portant l'analyse du pacte successoral global belge
 * (Loi 31/07/2017, CC art. 1100/1+). Outil single-country BE.
 */
@Entity
@Table(name = "pacte_successoral_be_2018_analyses")
@Getter
@Setter
public class PacteSuccessoralBe2018Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_signature_pacte", nullable = false)
    private LocalDate dateSignaturePacte;

    @Column(name = "acte_authentique", nullable = false)
    private boolean acteAuthentique;

    @Column(name = "accord_tous_heritiers_reservataires", nullable = false)
    private boolean accordTousHeritiersReservataires;

    @Column(name = "equilibre_donations_rapportables", nullable = false)
    private boolean equilibreDonationsRapportables;

    @Column(name = "presence_tous_heritiers_reservataires", nullable = false)
    private boolean presenceTousHeritiersReservataires;

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
