package fr.ailegalcase.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlePaymentRequired_returnsBodyWithCode() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getMethod()).thenReturn("POST");
        Mockito.when(req.getRequestURI()).thenReturn("/api/v1/case-files/123/analyze");

        PaymentRequiredException ex = new PaymentRequiredException(
                PaymentRequiredCode.TOKEN_BUDGET_EXCEEDED, "Budget tokens mensuel dépassé.");

        ResponseEntity<Map<String, String>> response = handler.handlePaymentRequired(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(response.getBody())
                .containsEntry("error", "402 PAYMENT_REQUIRED")
                .containsEntry("message", "Budget tokens mensuel dépassé.")
                .containsEntry("code", "TOKEN_BUDGET_EXCEEDED");
    }

    @Test
    void handlePaymentRequired_includesEachKnownCode() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getMethod()).thenReturn("POST");
        Mockito.when(req.getRequestURI()).thenReturn("/api/v1/whatever");

        for (PaymentRequiredCode code : PaymentRequiredCode.values()) {
            PaymentRequiredException ex = new PaymentRequiredException(code, "msg");
            ResponseEntity<Map<String, String>> response = handler.handlePaymentRequired(ex, req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
            assertThat(response.getBody()).containsEntry("code", code.name());
        }
    }

    @Test
    void handleResponseStatus_legacy402_doesNotIncludeCodeField() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getMethod()).thenReturn("GET");
        Mockito.when(req.getRequestURI()).thenReturn("/api/v1/legacy");

        ResponseStatusException legacy = new ResponseStatusException(
                HttpStatus.PAYMENT_REQUIRED, "Legacy quota error");

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatus(legacy, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(response.getBody())
                .containsEntry("error", "402 PAYMENT_REQUIRED")
                .containsEntry("message", "Legacy quota error")
                .doesNotContainKey("code");
    }

    @Test
    void handleResponseStatus_otherStatuses_unchanged() {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getMethod()).thenReturn("GET");
        Mockito.when(req.getRequestURI()).thenReturn("/api/v1/whatever");

        ResponseStatusException notFound = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        ResponseEntity<Map<String, String>> response = handler.handleResponseStatus(notFound, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .containsEntry("error", "404 NOT_FOUND")
                .containsEntry("message", "Not found")
                .doesNotContainKey("code");
    }

    @Test
    void paymentRequiredException_exposesCodeAndMessage() {
        PaymentRequiredException ex = new PaymentRequiredException(
                PaymentRequiredCode.SEAT_LIMIT_EXCEEDED, "Limite de sièges atteinte.");
        assertThat(ex.getCode()).isEqualTo(PaymentRequiredCode.SEAT_LIMIT_EXCEEDED);
        assertThat(ex.getMessage()).isEqualTo("Limite de sièges atteinte.");
    }
}
