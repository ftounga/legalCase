package fr.ailegalcase.shared;

/**
 * Exception métier signalant qu'un quota / budget plan est atteint.
 * Sérialisée en HTTP 402 par {@link GlobalExceptionHandler} avec un body
 * {@code {error, message, code}} où {@code code} permet au frontend de router
 * le rendu sans parser le message.
 */
public class PaymentRequiredException extends RuntimeException {

    private final PaymentRequiredCode code;

    public PaymentRequiredException(PaymentRequiredCode code, String message) {
        super(message);
        this.code = code;
    }

    public PaymentRequiredException(PaymentRequiredCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public PaymentRequiredCode getCode() {
        return code;
    }
}
