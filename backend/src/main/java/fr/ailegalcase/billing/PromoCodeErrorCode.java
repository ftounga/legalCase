package fr.ailegalcase.billing;

import org.springframework.http.HttpStatus;

/**
 * F-255 SF-255-01 — codes machine-readable des erreurs métier liées aux codes
 * promo. Sérialisés dans le body de la réponse HTTP par
 * {@link fr.ailegalcase.shared.GlobalExceptionHandler}, permettent au frontend
 * de router le rendu (toast, message contextuel) sans parser le message libre.
 */
public enum PromoCodeErrorCode {
    /** Création : un code identique existe déjà (uppercase). */
    PROMO_CODE_DUPLICATE(HttpStatus.CONFLICT),
    /** Accès super-admin requis pour l'opération demandée. */
    FORBIDDEN_SUPER_ADMIN(HttpStatus.FORBIDDEN),
    /** Redemption : le code saisi n'existe pas en table. */
    PROMO_CODE_NOT_FOUND(HttpStatus.NOT_FOUND),
    /** Redemption : le code a été désactivé par un super-admin. */
    PROMO_CODE_INACTIVE(HttpStatus.CONFLICT),
    /** Redemption : le code a passé sa date d'expiration. */
    PROMO_CODE_EXPIRED(HttpStatus.CONFLICT),
    /** Redemption : `uses_count` a atteint `max_uses`. */
    PROMO_CODE_EXHAUSTED(HttpStatus.CONFLICT),
    /** Redemption : le workspace n'est pas dans un essai gratuit actif. */
    WORKSPACE_NOT_TRIALING(HttpStatus.CONFLICT),
    /** Redemption : le workspace a déjà consommé un TRIAL_EXTENSION (invariant anti-abus). */
    WORKSPACE_ALREADY_REDEEMED_TRIAL_EXTENSION(HttpStatus.CONFLICT),
    /** Redemption : l'utilisateur authentifié n'est pas membre du workspace ciblé. */
    FORBIDDEN_WORKSPACE(HttpStatus.FORBIDDEN),
    /** Redemption : type STRIPE_DISCOUNT non implémenté en SF-01 (réservé à SF-255-04). */
    PROMO_CODE_TYPE_NOT_SUPPORTED_YET(HttpStatus.CONFLICT);

    private final HttpStatus status;

    PromoCodeErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
