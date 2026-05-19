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

    // SF-123-02 : total de seats facturés (membres actifs du workspace).
    // Synchronisé via StripeSeatService.syncSeatCount lors de
    // acceptInvitation / removeMember / webhook customer.subscription.updated.
    @Column(name = "seat_count", nullable = false)
    private int seatCount = 1;

    // SF-247-01 : true quand une résiliation est programmée pour la fin de
    // période de facturation Stripe (cancel_at_period_end). Reset à false par
    // le webhook customer.subscription.deleted au moment du downgrade FREE.
    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;

    // SF-247-01 : date de fin de la période de facturation Stripe en cours.
    // Renseigné depuis la réponse Stripe (current_period_end). Nullable :
    // un workspace FREE / sans abonnement Stripe n'a pas de période courante.
    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;
}
