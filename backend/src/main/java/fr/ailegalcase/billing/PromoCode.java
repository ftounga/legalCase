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
 * F-255 SF-255-01 — entité représentant un code promo administré par le
 * super-admin.
 *
 * <p>L'unicité de {@code code} est garantie par la contrainte SQL
 * {@code uq_promo_codes_code} (migration 299). La normalisation
 * (trim + uppercase) est appliquée côté service avant persistance pour
 * éviter les conflits dus à la casse.
 *
 * <p>{@code usesCount} est incrémenté atomiquement par
 * {@code PromoCodeRepository.incrementUsesCount} : pas d'écriture directe
 * via setter dans la logique de redemption (les setters restent disponibles
 * pour les tests / désactivation).
 */
@Entity
@Table(name = "promo_codes")
@Getter
@Setter
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromoCodeType type;

    @Column(name = "value_days")
    private Integer valueDays;

    @Column(name = "stripe_coupon_id", length = 255)
    private String stripeCouponId;

    /**
     * F-255 SF-255-04 — ID Stripe du {@code PromotionCode} créé en miroir du
     * code local pour les codes de type {@link PromoCodeType#STRIPE_DISCOUNT}.
     * Reste {@code null} pour TRIAL_EXTENSION. Indexé par migration 301 pour
     * le lookup webhook {@code customer.discount.created}.
     */
    @Column(name = "stripe_promotion_code_id", length = 255)
    private String stripePromotionCodeId;

    /**
     * F-255 SF-255-04 — type de réduction (PERCENT | AMOUNT) pour
     * {@link PromoCodeType#STRIPE_DISCOUNT}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_off_type", length = 10)
    private PromoCodeValueOffType valueOffType;

    /**
     * F-255 SF-255-04 — montant de réduction :
     * pourcentage 1..100 si {@link #valueOffType} = PERCENT,
     * centimes EUR si AMOUNT.
     */
    @Column(name = "value_off_amount")
    private Integer valueOffAmount;

    /**
     * F-255 SF-255-04 — devise (V1 : EUR uniquement). Requis si
     * {@link #valueOffType} = AMOUNT.
     */
    @Column(name = "currency", length = 3)
    private String currency;

    /**
     * F-255 SF-255-04 — durée d'application Stripe (ONCE | REPEATING_3 |
     * FOREVER). Requis pour {@link PromoCodeType#STRIPE_DISCOUNT}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "duration", length = 20)
    private PromoCodeDuration duration;

    @Column(name = "partner_label", nullable = false, length = 100)
    private String partnerLabel;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "uses_count", nullable = false)
    private int usesCount = 0;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
