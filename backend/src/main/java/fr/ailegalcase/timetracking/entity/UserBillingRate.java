package fr.ailegalcase.timetracking.entity;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.workspace.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_billing_rates")
@Getter
@Setter
public class UserBillingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerHour;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        this.createdAt = Instant.now();
    }
}
