package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prudhome_fiches")
@Getter
@Setter
public class PrudhomeFiche {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String demandeur = "{}";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String defendeur = "{}";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String demandes = "[]";

    @Column(columnDefinition = "TEXT")
    private String faitsTexte;

    @Column(columnDefinition = "TEXT")
    private String moyensDroitTexte;

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
