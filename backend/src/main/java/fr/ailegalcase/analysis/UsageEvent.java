package fr.ailegalcase.analysis;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usage_events")
@Getter
@Setter
public class UsageEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // F-257 SF-257-02 — nullable : les jobs SYSTEM_* (gate centralisé) n'ont ni
    // dossier ni utilisateur. Les lignes system restent exclues des budgets
    // workspace (INNER JOIN case_files dans UsageEventRepository).
    @Column(name = "case_file_id")
    private UUID caseFileId;

    @Column(name = "user_id")
    private UUID userId;

    // F-257 SF-257-02 — varchar(40) : SYSTEM_JURISPRUDENCE_VERIFICATION = 33 car.
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private JobType eventType;

    @Column(name = "tokens_input", nullable = false)
    private int tokensInput;

    @Column(name = "tokens_output", nullable = false)
    private int tokensOutput;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        this.createdAt = Instant.now();
    }
}
