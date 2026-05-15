package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.workspace.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_files")
@Getter
@Setter
public class CaseFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 50)
    private String legalDomain;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant lastDocumentDeletedAt;

    @Column
    private Instant deletedAt;

    /** F-243 : stade procédural — code juridiction du référentiel {@code ProcedureStageCatalog}. Nullable. */
    @Column(name = "procedure_jurisdiction", length = 50)
    private String procedureJurisdiction;

    /** F-243 : stade procédural — code stade rattaché à la juridiction. Nullable. */
    @Column(name = "procedure_stage", length = 50)
    private String procedureStage;

    /** F-243 : stade procédural — code position juridique valide pour le stade. Nullable. */
    @Column(name = "procedure_position", length = 50)
    private String procedurePosition;

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
