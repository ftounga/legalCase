package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deadline_alert_sends",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_deadline_alert_sends_deadline_threshold",
               columnNames = {"deadline_id", "threshold_days"}))
@Getter
@Setter
public class DeadlineAlertSend {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deadline_id", nullable = false)
    private CaseDeadline deadline;

    @Column(nullable = false)
    private int thresholdDays;

    @Column(nullable = false)
    private Instant sentAt;

    @PrePersist
    void onPrePersist() {
        this.sentAt = Instant.now();
    }
}
