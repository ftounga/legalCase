package fr.ailegalcase.referential;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referential_alerts")
@Getter
@Setter
public class ReferentialAlert {

    public enum Status { PENDING, APPLIED, DISMISSED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false)
    private LegalReferential entry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "proposed_value_json", nullable = false, columnDefinition = "TEXT")
    private String proposedValueJson;

    @Column(name = "ai_message", columnDefinition = "TEXT")
    private String aiMessage;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;
}
