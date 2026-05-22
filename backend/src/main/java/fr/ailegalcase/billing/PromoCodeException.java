package fr.ailegalcase.billing;

/**
 * F-255 SF-255-01 — exception métier portée par {@link PromoCodeErrorCode}.
 * Captée par {@link fr.ailegalcase.shared.GlobalExceptionHandler} qui
 * sérialise la réponse HTTP avec le {@code code} applicatif et le statut
 * porté par l'enum.
 */
public class PromoCodeException extends RuntimeException {

    private final PromoCodeErrorCode code;

    public PromoCodeException(PromoCodeErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public PromoCodeErrorCode getCode() {
        return code;
    }
}
