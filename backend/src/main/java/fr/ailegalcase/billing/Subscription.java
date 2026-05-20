package fr.ailegalcase.billing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
public class Subscription {

    /**
     * F-251 SF-251-02 — durée par défaut de la période d'évaluation FREE
     * appliquée par le garde-fou {@link #applyTrialExpiresAtFallback()}.
     * Aligné sur le calcul nominal dans
     * {@code WorkspaceService.createWorkspace}.
     */
    static final int FREE_TRIAL_DAYS = 14;

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

    /**
     * F-251 SF-251-02 — garde-fou JPA défensif.
     *
     * <p>Empêche la création d'une souscription FREE avec {@code expiresAt}
     * NULL : si le plan est FREE et que {@code expiresAt} n'est pas fourni,
     * on le calcule depuis {@code startedAt + 14 jours} (ou {@code now() + 14j}
     * si {@code startedAt} est lui aussi NULL).
     *
     * <p>Cas d'usage : un appel super-admin qui contournerait
     * {@code WorkspaceService.createWorkspace} et persisterait directement une
     * Subscription via le repository. Sans ce hook, le compte resterait FREE
     * sans date d'expiration → {@code PlanLimitService.isExpiredFree} renverrait
     * faux à perpétuité et l'IHM n'afficherait jamais la trial.
     *
     * <p>No-op pour :
     * <ul>
     *   <li>Plans payants (SOLO/TEAM/PRO) — leur cycle est piloté par Stripe.</li>
     *   <li>FREE avec {@code expiresAt} déjà fourni — comportement défensif,
     *       on respecte la valeur explicite.</li>
     * </ul>
     *
     * <p><b>Limite connue</b> : ne couvre pas les INSERT SQL directs (ils
     * contournent JPA). La skill {@code prospect-account-bootstrap} documente
     * la procédure opérateur — voir SF-251-03 (optionnelle).
     */
    @PrePersist
    void applyTrialExpiresAtFallback() {
        if (!"FREE".equals(planCode)) {
            return;
        }
        if (expiresAt != null) {
            return;
        }
        if (startedAt == null) {
            startedAt = Instant.now();
        }
        expiresAt = startedAt.plus(FREE_TRIAL_DAYS, ChronoUnit.DAYS);
    }
}
