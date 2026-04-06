package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tribunal_travail_fiches")
@Getter
@Setter
public class TribunalTravailFiche {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requerant = "{}";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String defendeur = "{}";

    @Column(name = "procedure_info", nullable = false, columnDefinition = "TEXT")
    private String procedureInfo = "{}";

    @Column(name = "contrat_info", nullable = false, columnDefinition = "TEXT")
    private String contratInfo = "{}";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String demandes = "[]";

    @Column(name = "expose_des_moyens", columnDefinition = "TEXT")
    private String exposeDesMoyens;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
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
