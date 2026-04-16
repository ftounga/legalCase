package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "indemnite_comparatifs")
@Getter
@Setter
public class IndemniteComparatif {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(nullable = false, length = 20)
    private String country;

    @Column(nullable = false)
    private int ancienneteAnnees;

    @Column(name = "anciennete_mois")
    private Integer ancienneteMois;

    @Column(nullable = false)
    private int age;

    @Column(name = "type_rupture", length = 50)
    private String typeRupture;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resultData = "{}";

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
