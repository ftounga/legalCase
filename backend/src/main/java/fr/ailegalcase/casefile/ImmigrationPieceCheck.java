package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "immigration_piece_checks",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_immigration_piece_checks_case_file_titre_label",
               columnNames = {"case_file_id", "titre_type", "country", "label"}))
@Getter
@Setter
public class ImmigrationPieceCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @Column(nullable = false, length = 50)
    private String titreType;

    @Column(nullable = false, length = 20)
    private String country;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(nullable = false, length = 20)
    private String statut = "INCONNU";

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
