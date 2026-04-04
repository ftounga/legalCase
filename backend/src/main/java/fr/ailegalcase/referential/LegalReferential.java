package fr.ailegalcase.referential;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legal_referentials")
@Getter
@Setter
public class LegalReferential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "legal_domain", nullable = false, length = 50)
    private String legalDomain;

    @Column(name = "referential_type", nullable = false, length = 100)
    private String referentialType;

    @Column(name = "entry_key", nullable = false, length = 200)
    private String entryKey;

    @Column(name = "value_json", nullable = false, columnDefinition = "TEXT")
    private String valueJson;

    @Column(length = 500)
    private String label;

    @Column(length = 20)
    private String country;

    @Column(name = "is_system", nullable = false)
    private boolean system = true;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "source_ref", length = 200)
    private String sourceRef;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by")
    private UUID updatedBy;
}
