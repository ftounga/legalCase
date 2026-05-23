package fr.ailegalcase.billing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * F-255 SF-255-01 + SF-255-04 — body de création d'un code promo (super-admin).
 *
 * <p>{@code valueDays} reste {@link Integer} (nullable) plutôt que primitif
 * pour distinguer absence d'envoi (STRIPE_DISCOUNT légitime) vs envoi à 0
 * (interdit par {@code @Min(1)}). La cohérence entre {@code type} et
 * {@code valueDays} / {@code valueOff*} / {@code duration} est vérifiée côté
 * service (non factorisable en annotation Bean Validation simple : règles
 * conditionnelles selon {@code type}).
 *
 * <p>SF-255-04 — pour {@link PromoCodeType#STRIPE_DISCOUNT}, les champs
 * {@code valueOffType}, {@code valueOffAmount} et {@code duration} sont
 * <strong>requis</strong> ; {@code currency} est requis uniquement si
 * {@code valueOffType=AMOUNT} (par défaut {@code EUR} côté service si absent).
 * {@code valueDays} est <strong>interdit</strong> pour STRIPE_DISCOUNT.
 */
public record PromoCodeCreateRequest(
        @NotBlank(message = "code is required")
        @Size(max = 64, message = "code must be at most 64 characters")
        @Pattern(regexp = "^[A-Za-z0-9_\\- ]+$",
                message = "code must match [A-Z0-9_-] (uppercase applied server-side)")
        String code,

        @NotNull(message = "type is required")
        PromoCodeType type,

        @Min(value = 1, message = "valueDays must be >= 1")
        @Max(value = 365, message = "valueDays must be <= 365")
        Integer valueDays,

        // SF-255-04 — branche STRIPE_DISCOUNT
        PromoCodeValueOffType valueOffType,

        @Min(value = 1, message = "valueOffAmount must be >= 1")
        @Max(value = 1_000_000, message = "valueOffAmount must be <= 1000000")
        Integer valueOffAmount,

        @Size(max = 3, message = "currency must be a 3-letter ISO code")
        String currency,

        PromoCodeDuration duration,

        @NotBlank(message = "partnerLabel is required")
        @Size(max = 100, message = "partnerLabel must be at most 100 characters")
        String partnerLabel,

        @NotNull(message = "maxUses is required")
        @Min(value = 1, message = "maxUses must be >= 1")
        @Max(value = 100_000, message = "maxUses must be <= 100000")
        Integer maxUses,

        @NotNull(message = "expiresAt is required")
        Instant expiresAt
) {

    /**
     * Constructeur de compatibilité descendante (SF-255-01) : permet aux
     * tests et appels existants TRIAL_EXTENSION de continuer à utiliser la
     * signature 6-args sans renseigner les champs Stripe (qui restent null).
     */
    public PromoCodeCreateRequest(String code,
                                  PromoCodeType type,
                                  Integer valueDays,
                                  String partnerLabel,
                                  Integer maxUses,
                                  Instant expiresAt) {
        this(code, type, valueDays, null, null, null, null,
                partnerLabel, maxUses, expiresAt);
    }
}
