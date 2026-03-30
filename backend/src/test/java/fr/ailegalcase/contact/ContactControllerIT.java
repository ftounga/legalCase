package fr.ailegalcase.contact;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class ContactControllerIT {

    @Autowired private MockMvc mockMvc;

    private static final String URL = "/api/v1/contact";

    // I-01 : payload complet (avec téléphone) → 200 + status sent
    @Test
    void contact_fullPayload_returns200() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Alice Dupont",
                                  "email": "alice@example.com",
                                  "telephone": "+33 6 12 34 56 78",
                                  "sujet": "Demande de démo",
                                  "message": "Bonjour, je souhaite une démonstration."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"));
    }

    // I-02 : payload minimal sans téléphone → 200
    @Test
    void contact_withoutPhone_returns200() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Bob Martin",
                                  "email": "bob@example.com",
                                  "sujet": "Question tarifaire",
                                  "message": "Quels sont vos tarifs ?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"));
    }

    // I-03 : nom absent → 400
    @Test
    void contact_missingNom_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "alice@example.com",
                                  "sujet": "Test",
                                  "message": "Message"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-04 : email absent → 400
    @Test
    void contact_missingEmail_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Alice",
                                  "sujet": "Test",
                                  "message": "Message"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-05 : email format invalide → 400
    @Test
    void contact_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Alice",
                                  "email": "pas-un-email",
                                  "sujet": "Test",
                                  "message": "Message"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-06 : sujet absent → 400
    @Test
    void contact_missingSujet_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Alice",
                                  "email": "alice@example.com",
                                  "message": "Message"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-07 : message absent → 400
    @Test
    void contact_missingMessage_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Alice",
                                  "email": "alice@example.com",
                                  "sujet": "Test"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-08 : téléphone format invalide → 400
    @Test
    void contact_invalidPhone_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Alice",
                                  "email": "alice@example.com",
                                  "telephone": "abc",
                                  "sujet": "Test",
                                  "message": "Message"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-09 : accessible sans token JWT (no auth required)
    @Test
    void contact_noAuth_isPermitted() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Anonyme",
                                  "email": "anon@example.com",
                                  "sujet": "Test public",
                                  "message": "Pas de token JWT"
                                }
                                """))
                .andExpect(status().isOk());
    }
}
