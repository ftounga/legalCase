package fr.ailegalcase.billing;

import com.stripe.exception.ApiException;
import com.stripe.model.Coupon;
import com.stripe.model.PromotionCode;
import com.stripe.param.CouponCreateParams;
import com.stripe.param.PromotionCodeCreateParams;
import com.stripe.param.PromotionCodeUpdateParams;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-255 SF-255-04 — tests unitaires du wrapper Stripe Coupons + PromotionCodes.
 */
class StripePromoCodeServiceTest {

    private StripePromoCodeService buildService(boolean enabled) {
        return new StripePromoCodeService(enabled, "sk_test_fake");
    }

    private PromoCode buildLocal(PromoCodeValueOffType type, int amount,
                                 PromoCodeDuration duration, String currency) {
        PromoCode c = new PromoCode();
        c.setId(UUID.randomUUID());
        c.setCode("TEST10");
        c.setType(PromoCodeType.STRIPE_DISCOUNT);
        c.setValueOffType(type);
        c.setValueOffAmount(amount);
        c.setCurrency(currency);
        c.setDuration(duration);
        c.setPartnerLabel("Partner");
        c.setMaxUses(50);
        c.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        c.setActive(true);
        c.setCreatedByUserId(UUID.randomUUID());
        return c;
    }

    // T-01 : création nominale percent_off
    @Test
    void createCouponAndPromotionCode_percentNominal_callsCouponAndPromotion() throws Exception {
        StripePromoCodeService service = buildService(true);
        PromoCode local = buildLocal(PromoCodeValueOffType.PERCENT, 10,
                PromoCodeDuration.ONCE, null);

        Coupon coupon = mock(Coupon.class);
        when(coupon.getId()).thenReturn("coupon_abc");
        PromotionCode promo = mock(PromotionCode.class);
        when(promo.getId()).thenReturn("promo_xyz");

        try (MockedStatic<Coupon> couponStatic = mockStatic(Coupon.class);
             MockedStatic<PromotionCode> promoStatic = mockStatic(PromotionCode.class)) {
            couponStatic.when(() -> Coupon.create(any(CouponCreateParams.class)))
                    .thenReturn(coupon);
            promoStatic.when(() -> PromotionCode.create(any(PromotionCodeCreateParams.class)))
                    .thenReturn(promo);

            String result = service.createCouponAndPromotionCode(local);

            assertThat(result).isEqualTo("promo_xyz");
            assertThat(local.getStripeCouponId()).isEqualTo("coupon_abc");

            ArgumentCaptor<CouponCreateParams> couponCap =
                    ArgumentCaptor.forClass(CouponCreateParams.class);
            couponStatic.verify(() -> Coupon.create(couponCap.capture()));
            CouponCreateParams cp = couponCap.getValue();
            assertThat(cp.getPercentOff()).isNotNull();
            assertThat(cp.getAmountOff()).isNull();
            assertThat(cp.getMaxRedemptions()).isEqualTo(50L);
            assertThat(cp.getDuration()).isEqualTo(CouponCreateParams.Duration.ONCE);

            ArgumentCaptor<PromotionCodeCreateParams> promoCap =
                    ArgumentCaptor.forClass(PromotionCodeCreateParams.class);
            promoStatic.verify(() -> PromotionCode.create(promoCap.capture()));
            PromotionCodeCreateParams pp = promoCap.getValue();
            assertThat(pp.getCoupon()).isEqualTo("coupon_abc");
            assertThat(pp.getCode()).isEqualTo("TEST10");
            assertThat(pp.getMaxRedemptions()).isEqualTo(50L);
            assertThat(pp.getExpiresAt()).isNotNull();
        }
    }

    // T-02 : création nominale amount_off + currency
    @Test
    void createCouponAndPromotionCode_amountNominal_callsWithCurrency() throws Exception {
        StripePromoCodeService service = buildService(true);
        PromoCode local = buildLocal(PromoCodeValueOffType.AMOUNT, 500,
                PromoCodeDuration.FOREVER, "EUR");

        Coupon coupon = mock(Coupon.class);
        when(coupon.getId()).thenReturn("coupon_amt");
        PromotionCode promo = mock(PromotionCode.class);
        when(promo.getId()).thenReturn("promo_amt");

        try (MockedStatic<Coupon> couponStatic = mockStatic(Coupon.class);
             MockedStatic<PromotionCode> promoStatic = mockStatic(PromotionCode.class)) {
            couponStatic.when(() -> Coupon.create(any(CouponCreateParams.class)))
                    .thenReturn(coupon);
            promoStatic.when(() -> PromotionCode.create(any(PromotionCodeCreateParams.class)))
                    .thenReturn(promo);

            service.createCouponAndPromotionCode(local);

            ArgumentCaptor<CouponCreateParams> couponCap =
                    ArgumentCaptor.forClass(CouponCreateParams.class);
            couponStatic.verify(() -> Coupon.create(couponCap.capture()));
            CouponCreateParams cp = couponCap.getValue();
            assertThat(cp.getAmountOff()).isEqualTo(500L);
            assertThat(cp.getCurrency()).isEqualTo("eur");
            assertThat(cp.getPercentOff()).isNull();
            assertThat(cp.getDuration()).isEqualTo(CouponCreateParams.Duration.FOREVER);
        }
    }

    // T-03 : duration REPEATING_3 → ajoute duration_in_months=3
    @Test
    void createCouponAndPromotionCode_repeating3_setsDurationInMonths() throws Exception {
        StripePromoCodeService service = buildService(true);
        PromoCode local = buildLocal(PromoCodeValueOffType.PERCENT, 20,
                PromoCodeDuration.REPEATING_3, null);

        Coupon coupon = mock(Coupon.class);
        when(coupon.getId()).thenReturn("coupon_r3");
        PromotionCode promo = mock(PromotionCode.class);
        when(promo.getId()).thenReturn("promo_r3");

        try (MockedStatic<Coupon> couponStatic = mockStatic(Coupon.class);
             MockedStatic<PromotionCode> promoStatic = mockStatic(PromotionCode.class)) {
            couponStatic.when(() -> Coupon.create(any(CouponCreateParams.class)))
                    .thenReturn(coupon);
            promoStatic.when(() -> PromotionCode.create(any(PromotionCodeCreateParams.class)))
                    .thenReturn(promo);

            service.createCouponAndPromotionCode(local);

            ArgumentCaptor<CouponCreateParams> couponCap =
                    ArgumentCaptor.forClass(CouponCreateParams.class);
            couponStatic.verify(() -> Coupon.create(couponCap.capture()));
            CouponCreateParams cp = couponCap.getValue();
            assertThat(cp.getDuration()).isEqualTo(CouponCreateParams.Duration.REPEATING);
            assertThat(cp.getDurationInMonths()).isEqualTo(3L);
        }
    }

    // T-04 : idempotence — si stripePromotionCodeId déjà set, no-op
    @Test
    void createCouponAndPromotionCode_alreadyHasId_idempotent() throws Exception {
        StripePromoCodeService service = buildService(true);
        PromoCode local = buildLocal(PromoCodeValueOffType.PERCENT, 10,
                PromoCodeDuration.ONCE, null);
        local.setStripePromotionCodeId("promo_existing");

        try (MockedStatic<Coupon> couponStatic = mockStatic(Coupon.class);
             MockedStatic<PromotionCode> promoStatic = mockStatic(PromotionCode.class)) {

            String result = service.createCouponAndPromotionCode(local);

            assertThat(result).isEqualTo("promo_existing");
            couponStatic.verifyNoInteractions();
            promoStatic.verifyNoInteractions();
        }
    }

    // T-05 : Stripe désactivé → IDs synthétiques (mode dev/test)
    @Test
    void createCouponAndPromotionCode_stripeDisabled_returnsLocalIds() throws Exception {
        StripePromoCodeService service = buildService(false);
        PromoCode local = buildLocal(PromoCodeValueOffType.PERCENT, 10,
                PromoCodeDuration.ONCE, null);

        try (MockedStatic<Coupon> couponStatic = mockStatic(Coupon.class);
             MockedStatic<PromotionCode> promoStatic = mockStatic(PromotionCode.class)) {

            String result = service.createCouponAndPromotionCode(local);

            assertThat(result).startsWith("promo_local_");
            assertThat(local.getStripeCouponId()).startsWith("coupon_local_");
            couponStatic.verifyNoInteractions();
            promoStatic.verifyNoInteractions();
        }
    }

    // T-06 : désactivation nominale
    @Test
    void deactivatePromotionCode_nominal_callsUpdateActiveFalse() throws Exception {
        StripePromoCodeService service = buildService(true);

        PromotionCode promo = mock(PromotionCode.class);

        try (MockedStatic<PromotionCode> promoStatic = mockStatic(PromotionCode.class)) {
            promoStatic.when(() -> PromotionCode.retrieve("promo_to_deactivate"))
                    .thenReturn(promo);
            when(promo.update(any(PromotionCodeUpdateParams.class))).thenReturn(promo);

            service.deactivatePromotionCode("promo_to_deactivate");

            ArgumentCaptor<PromotionCodeUpdateParams> capt =
                    ArgumentCaptor.forClass(PromotionCodeUpdateParams.class);
            verify(promo, times(1)).update(capt.capture());
            assertThat(capt.getValue().getActive()).isFalse();
        }
    }

    // T-07 : désactivation, Stripe échoue → log warning, pas d'exception
    @Test
    void deactivatePromotionCode_stripeFails_swallowsException() {
        StripePromoCodeService service = buildService(true);

        try (MockedStatic<PromotionCode> promoStatic = mockStatic(PromotionCode.class)) {
            promoStatic.when(() -> PromotionCode.retrieve("promo_kaput"))
                    .thenThrow(new ApiException("network down", "req_1", null, 500, null));

            assertThatCode(() -> service.deactivatePromotionCode("promo_kaput"))
                    .doesNotThrowAnyException();
        }
    }

    // T-08 : désactivation, ID null → no-op
    @Test
    void deactivatePromotionCode_nullId_isNoOp() {
        StripePromoCodeService service = buildService(true);
        assertThatCode(() -> service.deactivatePromotionCode(null))
                .doesNotThrowAnyException();
    }

    // T-09 : désactivation, Stripe désactivé → no-op
    @Test
    void deactivatePromotionCode_stripeDisabled_isNoOp() {
        StripePromoCodeService service = buildService(false);
        try (MockedStatic<PromotionCode> promoStatic = mockStatic(PromotionCode.class)) {
            service.deactivatePromotionCode("promo_x");
            promoStatic.verifyNoInteractions();
        }
    }

    // T-10 : mapping enum Duration
    @Test
    void mapDuration_coverage() {
        assertThat(StripePromoCodeService.mapDuration(PromoCodeDuration.ONCE))
                .isEqualTo(CouponCreateParams.Duration.ONCE);
        assertThat(StripePromoCodeService.mapDuration(PromoCodeDuration.REPEATING_3))
                .isEqualTo(CouponCreateParams.Duration.REPEATING);
        assertThat(StripePromoCodeService.mapDuration(PromoCodeDuration.FOREVER))
                .isEqualTo(CouponCreateParams.Duration.FOREVER);
        assertThat(StripePromoCodeService.mapDuration(null))
                .isEqualTo(CouponCreateParams.Duration.ONCE);
    }
}
