package fr.ailegalcase.observability;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientErrorLogControllerTest {

    private final ClientErrorLogService service = mock(ClientErrorLogService.class);
    private final ClientErrorLogController controller = new ClientErrorLogController(service);

    // U-01 : payload minimal accepté
    @Test
    void log_minimalPayload_returnsLogged() {
        when(service.tryLog(any(), any(), any())).thenReturn(true);
        ClientErrorPayload payload = new ClientErrorPayload("boom", null, null, null, null, null);
        HttpServletRequest req = mockRequest("1.2.3.4");

        ResponseEntity<Map<String, String>> response = controller.log(payload, null, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "logged");
        verify(service, times(1)).tryLog(eq(payload), isNull(), eq("1.2.3.4"));
    }

    // U-02 : payload complet, user authentifié → userKey utilisé, ipKey null
    @Test
    void log_authenticatedUser_usesUserKey() {
        when(service.tryLog(any(), any(), any())).thenReturn(true);
        OAuth2User user = mock(OAuth2User.class);
        when(user.getName()).thenReturn("user-42");
        ClientErrorPayload payload = new ClientErrorPayload("oops", "stack", "https://x", "Mozilla", "2026-05-21T00:00:00Z", "1.0.0");
        HttpServletRequest req = mockRequest("9.9.9.9");

        ResponseEntity<Map<String, String>> response = controller.log(payload, user, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).tryLog(eq(payload), eq("user-42"), isNull());
    }

    // U-03 : rate-limited → 429
    @Test
    void log_rateLimited_returns429() {
        when(service.tryLog(any(), any(), any())).thenReturn(false);
        ClientErrorPayload payload = new ClientErrorPayload("boom", null, null, null, null, null);
        HttpServletRequest req = mockRequest("1.2.3.4");

        ResponseEntity<Map<String, String>> response = controller.log(payload, null, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).containsEntry("status", "rate_limited");
    }

    // U-04 : service throw → 500 sans fuite
    @Test
    void log_serviceThrows_returns500() {
        when(service.tryLog(any(), any(), any())).thenThrow(new RuntimeException("internal"));
        ClientErrorPayload payload = new ClientErrorPayload("boom", null, null, null, null, null);
        HttpServletRequest req = mockRequest("1.2.3.4");

        ResponseEntity<Map<String, String>> response = controller.log(payload, null, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", "error");
        // pas de message d'erreur fuité
        assertThat(response.getBody().toString()).doesNotContain("internal");
    }

    // U-05 : X-Forwarded-For prioritaire (premier IP)
    @Test
    void log_xForwardedFor_usesFirstIp() {
        when(service.tryLog(any(), any(), any())).thenReturn(true);
        ClientErrorPayload payload = new ClientErrorPayload("boom", null, null, null, null, null);
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");

        controller.log(payload, null, req);

        verify(service).tryLog(eq(payload), isNull(), eq("10.0.0.1"));
    }

    private static HttpServletRequest mockRequest(String remoteAddr) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn(remoteAddr);
        return req;
    }
}
