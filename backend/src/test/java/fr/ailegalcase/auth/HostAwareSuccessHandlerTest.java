package fr.ailegalcase.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HostAwareSuccessHandlerTest {

    private static final String FALLBACK = "https://legalcase.fr";
    private static final List<String> ALLOWED = List.of(
            "https://legalcase.fr",
            "https://staging.legalcase.fr",
            "https://legalcase.ng-itconsulting.com",
            "https://staging.legalcase.ng-itconsulting.com"
    );

    private HostAwareSuccessHandler handler;

    @BeforeEach
    void setup() {
        handler = new HostAwareSuccessHandler(ALLOWED, FALLBACK);
    }

    @Test
    void redirects_to_origin_when_request_host_is_allowed_new_staging() throws Exception {
        MockHttpServletResponse response = invokeHandler("https", "staging.legalcase.fr", 443);
        assertThat(response.getRedirectedUrl()).isEqualTo("https://staging.legalcase.fr/dashboard");
    }

    @Test
    void redirects_to_origin_when_request_host_is_allowed_legacy_staging() throws Exception {
        MockHttpServletResponse response = invokeHandler("https", "staging.legalcase.ng-itconsulting.com", 443);
        assertThat(response.getRedirectedUrl()).isEqualTo("https://staging.legalcase.ng-itconsulting.com/dashboard");
    }

    @Test
    void redirects_to_origin_when_request_host_is_allowed_new_prod() throws Exception {
        MockHttpServletResponse response = invokeHandler("https", "legalcase.fr", 443);
        assertThat(response.getRedirectedUrl()).isEqualTo("https://legalcase.fr/dashboard");
    }

    @Test
    void redirects_to_origin_when_request_host_is_allowed_legacy_prod() throws Exception {
        MockHttpServletResponse response = invokeHandler("https", "legalcase.ng-itconsulting.com", 443);
        assertThat(response.getRedirectedUrl()).isEqualTo("https://legalcase.ng-itconsulting.com/dashboard");
    }

    @Test
    void falls_back_to_default_when_request_host_is_not_allowed() throws Exception {
        MockHttpServletResponse response = invokeHandler("https", "malicious.example.com", 443);
        assertThat(response.getRedirectedUrl()).isEqualTo("https://legalcase.fr/dashboard");
    }

    @Test
    void includes_port_when_not_default_for_scheme() throws Exception {
        HostAwareSuccessHandler localHandler = new HostAwareSuccessHandler(
                List.of("http://localhost:8080"), FALLBACK);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);

        MockHttpServletResponse response = new MockHttpServletResponse();
        localHandler.onAuthenticationSuccess(request, response, null);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:8080/dashboard");
    }

    private MockHttpServletResponse invokeHandler(String scheme, String host, int port) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme(scheme);
        request.setServerName(host);
        request.setServerPort(port);

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, null);
        return response;
    }
}
