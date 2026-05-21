package fr.ailegalcase.observability;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class ClientErrorLogControllerIT {

    @Autowired private MockMvc mockMvc;

    private static final String URL = "/api/v1/logs/client-error";

    // I-01 : payload valide anonyme → 200 + status logged
    @Test
    void clientError_validAnonymous_returnsLogged() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "TypeError: cannot read property 'x' of undefined",
                                  "stack": "at App.doSomething (app.js:42)",
                                  "url": "https://staging.legalcase.fr/case-files/abc",
                                  "userAgent": "Mozilla/5.0",
                                  "timestamp": "2026-05-21T10:00:00Z",
                                  "appVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("logged"));
    }

    // I-02 : payload minimal (message seul) → 200
    @Test
    void clientError_minimalPayload_returnsLogged() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "boom"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("logged"));
    }

    // I-03 : message absent → 400
    @Test
    void clientError_missingMessage_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stack": "at x"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-04 : message > 500 chars → 400
    @Test
    void clientError_messageTooLong_returns400() throws Exception {
        String tooLong = "x".repeat(501);
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "%s"
                                }
                                """.formatted(tooLong)))
                .andExpect(status().isBadRequest());
    }

    // I-05 : stack > 4000 chars → 400
    @Test
    void clientError_stackTooLong_returns400() throws Exception {
        String tooLong = "y".repeat(4001);
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "ok",
                                  "stack": "%s"
                                }
                                """.formatted(tooLong)))
                .andExpect(status().isBadRequest());
    }

    // I-06 : endpoint accessible sans authentification (permitAll)
    @Test
    void clientError_noAuth_isPermitted() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "anonymous error"
                                }
                                """))
                .andExpect(status().isOk());
    }
}
