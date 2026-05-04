package fr.ailegalcase.superadmin;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-187 SF-187-01 — Tests d'intégration du endpoint
 * {@code POST /api/v1/super-admin/traction-onepager/pdf}.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class TractionOnePagerControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private ObjectMapper objectMapper;

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

    /** IT-01 : super-admin → 200 + Content-Type pdf + body ≥ 1 KB. */
    @Test
    void exportTractionOnePager_withSuperAdmin_returns200WithPdf() throws Exception {
        User superAdmin = createSuperAdmin("superadmin-traction1@example.com", "google-traction1-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth("google-traction1-sub", superAdmin.getEmail());

        Map<String, Object> body = validBody();

        MvcResult res = mockMvc.perform(post("/api/v1/super-admin/traction-onepager/pdf")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        byte[] pdf = res.getResponse().getContentAsByteArray();
        assertThat(pdf.length).isGreaterThanOrEqualTo(1024);
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF-");
    }

    /** IT-02 : utilisateur normal authentifié → 403. */
    @Test
    void exportTractionOnePager_withoutSuperAdmin_returns403() throws Exception {
        User user = createRegular("regular-traction@example.com", "google-traction-regular-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth("google-traction-regular-sub", user.getEmail());

        mockMvc.perform(post("/api/v1/super-admin/traction-onepager/pdf")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF)
                        .content(objectMapper.writeValueAsBytes(validBody())))
                .andExpect(status().isForbidden());
    }

    /** IT-03 : non authentifié → 401. */
    @Test
    void exportTractionOnePager_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/super-admin/traction-onepager/pdf")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF)
                        .content(objectMapper.writeValueAsBytes(validBody())))
                .andExpect(status().isUnauthorized());
    }

    /** IT-04 : headline vide → 400. */
    @Test
    void exportTractionOnePager_emptyHeadline_returns400() throws Exception {
        User superAdmin = createSuperAdmin("superadmin-traction4@example.com", "google-traction4-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth("google-traction4-sub", superAdmin.getEmail());

        Map<String, Object> body = validBody();
        body.put("headline", "   "); // blanc après trim

        mockMvc.perform(post("/api/v1/super-admin/traction-onepager/pdf")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest());
    }

    /** IT-05 : verbatims.length = 3 → 400. */
    @Test
    void exportTractionOnePager_tooManyVerbatims_returns400() throws Exception {
        User superAdmin = createSuperAdmin("superadmin-traction5@example.com", "google-traction5-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth("google-traction5-sub", superAdmin.getEmail());

        Map<String, Object> body = validBody();
        body.put("verbatims", List.of(
                Map.of("quote", "q1", "author", "a1"),
                Map.of("quote", "q2", "author", "a2"),
                Map.of("quote", "q3", "author", "a3")));

        mockMvc.perform(post("/api/v1/super-admin/traction-onepager/pdf")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest());
    }

    /** IT-06 : partners.length = 11 → 400. */
    @Test
    void exportTractionOnePager_tooManyPartners_returns400() throws Exception {
        User superAdmin = createSuperAdmin("superadmin-traction6@example.com", "google-traction6-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth("google-traction6-sub", superAdmin.getEmail());

        Map<String, Object> body = validBody();
        body.put("partners", List.of(
                "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10", "p11"));

        mockMvc.perform(post("/api/v1/super-admin/traction-onepager/pdf")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest());
    }

    /** IT-07 : contact.email invalide → 400. */
    @Test
    void exportTractionOnePager_invalidEmail_returns400() throws Exception {
        User superAdmin = createSuperAdmin("superadmin-traction7@example.com", "google-traction7-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth("google-traction7-sub", superAdmin.getEmail());

        Map<String, Object> body = validBody();
        body.put("contact", Map.of(
                "name", "Franck",
                "email", "pas-un-email",
                "url", "https://legalcase.example.com"));

        mockMvc.perform(post("/api/v1/super-admin/traction-onepager/pdf")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest());
    }

    /** IT-08 : verbatim.quote > 500 chars → 400. */
    @Test
    void exportTractionOnePager_verbatimQuoteTooLong_returns400() throws Exception {
        User superAdmin = createSuperAdmin("superadmin-traction8@example.com", "google-traction8-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth("google-traction8-sub", superAdmin.getEmail());

        Map<String, Object> body = validBody();
        String oversized = "x".repeat(501);
        body.put("verbatims", List.of(
                Map.of("quote", oversized, "author", "Auteur")));

        mockMvc.perform(post("/api/v1/super-admin/traction-onepager/pdf")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isBadRequest());
    }

    /** IT-09 : header Content-Disposition contient le nom de fichier daté du jour. */
    @Test
    void exportTractionOnePager_contentDispositionContainsTodayFilename() throws Exception {
        User superAdmin = createSuperAdmin("superadmin-traction9@example.com", "google-traction9-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth("google-traction9-sub", superAdmin.getEmail());

        String expectedDate = LocalDate.now(ZoneId.of("UTC")).format(ISO_DATE);
        String expectedFilename = "legalcase-traction-" + expectedDate + ".pdf";

        mockMvc.perform(post("/api/v1/super-admin/traction-onepager/pdf")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF)
                        .content(objectMapper.writeValueAsBytes(validBody())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"" + expectedFilename + "\""))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private User createSuperAdmin(String email, String sub) {
        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");
        user.setSuperAdmin(true);
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        authAccountRepository.save(account);
        return user;
    }

    private User createRegular(String email, String sub) {
        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");
        user.setSuperAdmin(false);
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        authAccountRepository.save(account);
        return user;
    }

    private OAuth2AuthenticationToken buildGoogleAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub,
                "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }

    /** Construit un body JSON valide minimal (mutable pour customiser dans chaque test). */
    private Map<String, Object> validBody() {
        Map<String, Object> includeKpis = new java.util.HashMap<>();
        includeKpis.put("totalWorkspaces", true);
        includeKpis.put("trialWorkspaces", true);
        includeKpis.put("paidWorkspaces", true);
        includeKpis.put("conversionRatePct", true);
        includeKpis.put("activeWorkspaces30d", true);
        includeKpis.put("analysesLast7Days", true);
        includeKpis.put("analysesLast30Days", true);

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("includeKpis", includeKpis);
        body.put("verbatims", List.of(
                Map.of("quote", "Citation de test", "author", "Auteur Test")));
        body.put("partners", List.of("Partner 1", "Partner 2"));
        body.put("headline", "Headline test");
        body.put("contact", Map.of(
                "name", "Franck Tounga",
                "email", "franck@ng-itconsulting.com",
                "url", "https://legalcase.ng-itconsulting.com"));
        return body;
    }
}
