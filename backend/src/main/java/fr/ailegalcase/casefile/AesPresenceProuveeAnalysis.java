package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-214-11 : entity 1:1 par dossier portant l'analyse du calcul de présence
 * prouvée en France (circulaire Valls 28/11/2012) et de l'éligibilité aux 4 voies
 * AES (famille 5 ans / humanitaire 10 ans / étudiant 3 ans / métiers en tension
 * 3 ans — L. 435-1 et L. 435-3 CESEDA). Outil single-country FR.
 *
 * <p>Les périodes présentées (avec pièce justificative) sont persistées en JSON
 * dans {@code result_data} via {@link AesPresenceProuveeResult}.</p>
 */
@Entity
@Table(name = "aes_presence_prouvee_analyses")
@Getter
@Setter
public class AesPresenceProuveeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "annees_totales_prouvees", nullable = false)
    private int anneesTotalesProuvees;

    @Column(name = "mois_totaux_prouves", nullable = false)
    private int moisTotauxProuves;

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
