package fr.ailegalcase.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-255 SF-255-01 — journal d'audit immuable d'une redemption de code promo.
 *
 * <p>{@code codeAtRedemption} et {@code valueAppliedDays} sont des snapshots
 * pris au moment T : si le code source est renommé ou modifié en V2, l'audit
 * reste lisible et reflète exactement l'effet appliqué.
 */
@Entity
@Table(name = "promo_code_redemptions")
@Getter
@Setter
public class PromoCodeRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "promo_code_id", nullable = false)
    private UUID promoCodeId;

    @Column(name = "code_at_redemption", nullable = false, length = 64)
    private String codeAtRedemption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromoCodeType type;

    @Column(name = "value_applied_days")
    private Integer valueAppliedDays;

    /**
     * F-255 SF-255-04 — centimes EUR de réduction effectivement appliqués
     * lors d'une redemption {@link PromoCodeType#STRIPE_DISCOUNT}. Reste
     * {@code null} pour TRIAL_EXTENSION (la valeur pertinente est alors
     * {@link #valueAppliedDays}).
     */
    @Column(name = "value_applied_amount")
    private Integer valueAppliedAmount;

    /**
     * F-255 SF-255-04 — ID de l'événement Stripe (ex. {@code evt_…}) à
     * l'origine de la redemption. Sert de garde-fou d'idempotence webhook :
     * contrainte UNIQUE (nullable) au niveau DB → si Stripe ré-émet le même
     * event, l'INSERT échoue et le service no-op. Reste {@code null} pour
     * TRIAL_EXTENSION (pas de webhook).
     */
    @Column(name = "source_event_id", length = 255)
    private String sourceEventId;

    @Column(name = "redeemed_at", nullable = false)
    private Instant redeemedAt;

    @Column(name = "applied_by_user_id", nullable = false)
    private UUID appliedByUserId;

    @PrePersist
    void onPrePersist() {
        if (redeemedAt == null) {
            redeemedAt = Instant.now();
        }
    }
}
