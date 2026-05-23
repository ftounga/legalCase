package fr.ailegalcase.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Coupon;
import com.stripe.model.PromotionCode;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.PromotionCodeCreateParams;
import com.stripe.param.PromotionCodeUpdateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * F-255 SF-255-04 — wrapper Stripe pour la création et la désactivation des
 * {@link com.stripe.model.Coupon} + {@link com.stripe.model.PromotionCode}
 * miroirs d'un {@link PromoCode} local de type
 * {@link PromoCodeType#STRIPE_DISCOUNT}.
 *
 * <p>Séparé de {@link StripeCheckoutService} pour respecter SRP : un service
 * = une responsabilité Stripe (checkout / customer / coupons).
 *
 * <p>Idempotence à la création : si {@link PromoCode#getStripePromotionCodeId()}
 * est déjà renseigné, on retourne directement l'ID existant sans rappeler
 * Stripe.
 *
 * <p>La désactivation est <strong>best-effort</strong> — un échec Stripe ne
 * doit pas bloquer la désactivation locale (qui suffit fonctionnellement).
 */
@Service
public class StripePromoCodeService {

    private static final Logger log = LoggerFactory.getLogger(StripePromoCodeService.class);
    private static final String DEFAULT_CURRENCY = "EUR";

    private final boolean stripeEnabled;
    private final String secretKey;

    public StripePromoCodeService(
            @Value("${app.stripe.enabled:false}") boolean stripeEnabled,
            @Value("${app.stripe.secret-key:}") String secretKey) {
        this.stripeEnabled = stripeEnabled;
        this.secretKey = secretKey;
    }

    /**
     * Crée un {@link Coupon} puis un {@link PromotionCode} chez Stripe.
     *
     * <p>Renseigne {@link PromoCode#setStripeCouponId(String)} et retourne
     * l'ID du {@code PromotionCode} (à persister dans
     * {@link PromoCode#setStripePromotionCodeId(String)} par l'appelant).
     *
     * <p>Idempotent : si {@code local.getStripePromotionCodeId() != null},
     * retourne directement cette valeur sans rappeler Stripe.
     *
     * @throws StripeException si l'un des deux appels Stripe échoue. L'appelant
     *         doit traduire en {@link PromoCodeException} avec le code
     *         {@link PromoCodeErrorCode#STRIPE_API_UNAVAILABLE}.
     */
    public String createCouponAndPromotionCode(PromoCode local) throws StripeException {
        if (local.getStripePromotionCodeId() != null && !local.getStripePromotionCodeId().isBlank()) {
            log.info("StripePromoCode action=CREATE skipped (idempotent) code={} existing promoId={}",
                    local.getCode(), local.getStripePromotionCodeId());
            return local.getStripePromotionCodeId();
        }
        if (!stripeEnabled) {
            // En environnement dev / test sans Stripe : on ne fail pas, on
            // simule des IDs déterministes pour pouvoir continuer le flow.
            String fakeCoupon = "coupon_local_" + local.getCode();
            String fakePromo = "promo_local_" + local.getCode();
            local.setStripeCouponId(fakeCoupon);
            log.info("StripePromoCode action=CREATE stripeDisabled — synthesizing local IDs code={} coupon={} promo={}",
                    local.getCode(), fakeCoupon, fakePromo);
            return fakePromo;
        }

        Stripe.apiKey = secretKey;
        CouponCreateParams couponParams = buildCouponParams(local);
        Coupon coupon = Coupon.create(couponParams);
        local.setStripeCouponId(coupon.getId());

        PromotionCodeCreateParams promoParams = buildPromotionCodeParams(local, coupon.getId());
        PromotionCode promo = PromotionCode.create(promoParams);
        log.info("StripePromoCode action=CREATE code={} couponId={} promotionCodeId={}",
                local.getCode(), coupon.getId(), promo.getId());
        return promo.getId();
    }

    /**
     * Désactive un {@link PromotionCode} côté Stripe (best-effort).
     *
     * <p>Si Stripe est désactivé ou si l'appel échoue, on log un warning mais
     * on ne lève pas : la désactivation locale est suffisante côté UX (le
     * code est marqué inactif en DB, on l'empêche d'être réutilisé via Stripe
     * Checkout au prochain billing cycle).
     */
    public void deactivatePromotionCode(String promotionCodeId) {
        if (promotionCodeId == null || promotionCodeId.isBlank()) {
            return;
        }
        if (!stripeEnabled) {
            log.debug("StripePromoCode action=DEACTIVATE skipped stripeDisabled promoId={}", promotionCodeId);
            return;
        }
        try {
            Stripe.apiKey = secretKey;
            PromotionCode promo = PromotionCode.retrieve(promotionCodeId);
            PromotionCodeUpdateParams params = PromotionCodeUpdateParams.builder()
                    .setActive(false)
                    .build();
            promo.update(params);
            log.info("StripePromoCode action=DEACTIVATE promoId={}", promotionCodeId);
        } catch (StripeException e) {
            log.warn("StripePromoCode action=DEACTIVATE failed (best-effort) promoId={} reason={}",
                    promotionCodeId, e.getMessage());
        }
    }

    private CouponCreateParams buildCouponParams(PromoCode local) {
        CouponCreateParams.Builder builder = CouponCreateParams.builder()
                .setName(local.getPartnerLabel())
                .setDuration(mapDuration(local.getDuration()))
                .putMetadata("partner_label", local.getPartnerLabel())
                .putMetadata("legalcase_code", local.getCode());

        if (local.getMaxUses() > 0) {
            builder.setMaxRedemptions((long) local.getMaxUses());
        }
        if (local.getDuration() == PromoCodeDuration.REPEATING_3) {
            builder.setDurationInMonths(3L);
        }

        if (local.getValueOffType() == PromoCodeValueOffType.PERCENT) {
            builder.setPercentOff(BigDecimal.valueOf(local.getValueOffAmount()));
        } else if (local.getValueOffType() == PromoCodeValueOffType.AMOUNT) {
            builder.setAmountOff((long) local.getValueOffAmount());
            String currency = local.getCurrency() != null && !local.getCurrency().isBlank()
                    ? local.getCurrency().toLowerCase(Locale.ROOT)
                    : DEFAULT_CURRENCY.toLowerCase(Locale.ROOT);
            builder.setCurrency(currency);
        }
        return builder.build();
    }

    private PromotionCodeCreateParams buildPromotionCodeParams(PromoCode local, String couponId) {
        PromotionCodeCreateParams.Builder builder = PromotionCodeCreateParams.builder()
                .setCoupon(couponId)
                .setCode(local.getCode())
                .setActive(local.isActive());

        if (local.getMaxUses() > 0) {
            builder.setMaxRedemptions((long) local.getMaxUses());
        }
        if (local.getExpiresAt() != null) {
            builder.setExpiresAt(local.getExpiresAt().getEpochSecond());
        }
        return builder.build();
    }

    static CouponCreateParams.Duration mapDuration(PromoCodeDuration duration) {
        if (duration == null) {
            // Garde-fou : la validation côté PromoCodeService doit l'avoir
            // rejeté, mais on retombe sur ONCE plutôt que de NPE.
            return CouponCreateParams.Duration.ONCE;
        }
        return switch (duration) {
            case ONCE -> CouponCreateParams.Duration.ONCE;
            case REPEATING_3 -> CouponCreateParams.Duration.REPEATING;
            case FOREVER -> CouponCreateParams.Duration.FOREVER;
        };
    }
}
