package fr.ailegalcase.billing;

import java.time.Instant;
import java.util.UUID;

/**
 * F-255 SF-255-01 — projection lecture d'un {@link PromoCode} pour les
 * endpoints super-admin. {@code usesCount} est recalculé depuis
 * {@code promo_code_redemptions} au moment du listing pour garantir 100 %
 * de cohérence avec l'audit (cf. C4).
 */
public record PromoCodeDto(
        UUID id,
        String code,
        PromoCodeType type,
        Integer valueDays,
        String stripeCouponId,
        String partnerLabel,
        int maxUses,
        long usesCount,
        Instant expiresAt,
        boolean active,
        Instant createdAt,
        UUID createdByUserId
) {

    public static PromoCodeDto from(PromoCode code, long usesCount) {
        return new PromoCodeDto(
                code.getId(),
                code.getCode(),
                code.getType(),
                code.getValueDays(),
                code.getStripeCouponId(),
                code.getPartnerLabel(),
                code.getMaxUses(),
                usesCount,
                code.getExpiresAt(),
                code.isActive(),
                code.getCreatedAt(),
                code.getCreatedByUserId()
        );
    }
}
