package fr.ailegalcase.casefile.conclusion;

/**
 * F-98 / SF-98-01 — exception levée par {@code CaseConclusionCommandService} quand
 * une garde du déclenchement échoue. Mappée en {@code 409} par
 * {@code CaseConclusionExceptionHandler} avec le corps
 * {@code {"error": "<code>", "message": "<texte avocat>"}}.
 */
public class CaseConclusionGuardException extends RuntimeException {

    private final transient CaseConclusionGuardCode code;

    public CaseConclusionGuardException(CaseConclusionGuardCode code) {
        super(code.message());
        this.code = code;
    }

    public CaseConclusionGuardCode getCode() {
        return code;
    }
}
