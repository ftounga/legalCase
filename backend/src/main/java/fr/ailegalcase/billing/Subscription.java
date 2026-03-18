package fr.ailegalcase.billing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false, unique = true)
    private UUID workspaceId;

    @Column(nullable = false, length = 20)
    private String planCode;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant expiresAt;

    @Column(length = 255)
    private String stripeCustomerId;

    @Column(length = 255)
    private String stripeSubscriptionId;
}
