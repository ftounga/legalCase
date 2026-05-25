package fr.ailegalcase.superadmin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-251 SF-251-03 — IT pour {@code POST /api/v1/super-admin/prospect-bootstrap}.
 *
 * <p>Test central : {@link #bootstrap_superAdminPayloadValide_jette201_etSubscriptionExpiresAtNonNull}
 * prouve que le bug Renversez 13/05 (Subscription FREE avec {@code expires_at}
 * NULL) ne peut plus se reproduire via ce chemin — la subscription créée a
 * toujours un {@code expires_at} ≈ {@code started_at + 14 jours}.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class SuperAdminProspectBootstrapControllerIT {

    private static final String URL = "/api/v1/super-admin/prospect-bootstrap";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static String payloadJson(String email, String password, String workspaceName) {
        return """
                {
                  "firstName": "Marjolaine",
                  "lastName": "RENVERSEZ",
                  "email": "%s",
                  "password": "%s",
                  "country": "FRANCE",
                  "legalDomain": "DROIT_DU_TRAVAIL",
                  "workspaceName": "%s"
                }
                """.formatted(email, password, workspaceName);
    }

    private User createSuperAdmin(String email, String googleSub) {
        User superAdmin = new User();
        superAdmin.setEmail(email);
        superAdmin.setStatus("ACTIVE");
        superAdmin.setSuperAdmin(true);
        userRepository.save(superAdmin);

        AuthAccount account = new AuthAccount();
        account.setUser(superAdmin);
        account.setProvider("GOOGLE");
        account.setProviderUserId(googleSub);
        authAccountRepository.save(account);
        return superAdmin;
    }

    private User createRegularUser(String email, String googleSub) {
        User regular = new User();
        regular.setEmail(email);
        regular.setStatus("ACTIVE");
        regular.setSuperAdmin(false);
        userRepository.save(regular);

        AuthAccount account = new AuthAccount();
        account.setUser(regular);
        account.setProvider("GOOGLE");
        account.setProviderUserId(googleSub);
        authAccountRepository.save(account);
        return regular;
    }

    private OAuth2AuthenticationToken buildGoogleAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub,
                "email", email,
                "iss", "https://accounts.google.com"
        );
        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }

    // IT-01 : non authentifié → 401
    @Test
    void bootstrap_nonAuth_jette401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("prospect-401@example.com", "printemps2026", "PROSPECT-401")))
                .andExpect(status().isUnauthorized());
    }

    // IT-02 : authentifié mais non super-admin → 403
    @Test
    void bootstrap_authNonSuperAdmin_jette403() throws Exception {
        createRegularUser("regular-bootstrap@example.com", "google-regular-bootstrap-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth(
                "google-regular-bootstrap-sub", "regular-bootstrap@example.com");

        mockMvc.perform(post(URL)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("prospect-403@example.com", "printemps2026", "PROSPECT-403")))
                .andExpect(status().isForbidden());
    }

    // IT-03 : LE CŒUR — super-admin + payload valide → 201 + subscription.expiresAt non NULL ≈ now+14j
    // Cette assertion prouve que le bug Renversez (expires_at NULL) ne peut plus se reproduire.
    @Test
    void bootstrap_superAdminPayloadValide_jette201_etSubscriptionExpiresAtNonNull() throws Exception {
        createSuperAdmin("superadmin-bootstrap@example.com", "google-superadmin-bootstrap-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth(
                "google-superadmin-bootstrap-sub", "superadmin-bootstrap@example.com");

        Instant beforeCall = Instant.now();

        MvcResult result = mockMvc.perform(post(URL)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("renversez-it@example.com", "printemps2026", "RENVERSEZ-IT")))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.has("userId")).isTrue();
        assertThat(json.has("workspaceId")).isTrue();
        assertThat(json.get("workspaceName").asText()).isEqualTo("RENVERSEZ-IT");
        assertThat(json.get("expiresAt").asText()).isNotBlank();

        UUID workspaceId = UUID.fromString(json.get("workspaceId").asText());
        UUID userId = UUID.fromString(json.get("userId").asText());

        // Vérification DB : User + AuthAccount LOCAL + Workspace + Membership + Subscription
        User createdUser = userRepository.findById(userId).orElseThrow();
        assertThat(createdUser.getEmail()).isEqualTo("renversez-it@example.com");
        assertThat(createdUser.getStatus()).isEqualTo("ACTIVE");

        AuthAccount localAccount = authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", "renversez-it@example.com")
                .orElseThrow();
        assertThat(localAccount.getUser().getId()).isEqualTo(userId);
        assertThat(localAccount.isEmailVerified()).isTrue();
        assertThat(passwordEncoder.matches("printemps2026", localAccount.getPasswordHash())).isTrue();

        Workspace workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        assertThat(workspace.getName()).isEqualTo("RENVERSEZ-IT");
        assertThat(workspace.getStatus()).isEqualTo("ACTIVE");
        assertThat(workspace.getPlanCode()).isEqualTo("FREE");
        assertThat(workspace.getLegalDomain()).isEqualTo("DROIT_DU_TRAVAIL");
        assertThat(workspace.getCountry()).isEqualTo("FRANCE");

        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspace_Id(workspaceId);
        assertThat(members).hasSize(1);
        assertThat(members.get(0).getUser().getId()).isEqualTo(userId);
        assertThat(members.get(0).getMemberRole()).isEqualTo("OWNER");
        assertThat(members.get(0).isPrimary()).isTrue();

        // **Assertion centrale F-251 SF-251-03** : la subscription doit avoir un
        // expiresAt non NULL ≈ started_at + 14 jours (preuve que le bug Renversez
        // ne peut pas être reproduit via cet endpoint).
        Subscription subscription = subscriptionRepository.findByWorkspaceId(workspaceId).orElseThrow();
        assertThat(subscription.getPlanCode()).isEqualTo("FREE");
        assertThat(subscription.getStatus()).isEqualTo("ACTIVE");
        assertThat(subscription.getStartedAt())
                .as("started_at posé par WorkspaceService.createWorkspaceForBootstrappedUser")
                .isNotNull()
                .isAfterOrEqualTo(beforeCall.minusSeconds(1));
        assertThat(subscription.getExpiresAt())
                .as("CŒUR F-251 SF-251-03 : expiresAt non NULL garantit que le bug Renversez ne se reproduit pas")
                .isNotNull()
                .isCloseTo(subscription.getStartedAt().plus(14, ChronoUnit.DAYS),
                        within(5, ChronoUnit.SECONDS));
    }

    // IT-04 : email déjà actif (≥ 1 WorkspaceMember) → 409
    @Test
    void bootstrap_emailDejaActif_jette409() throws Exception {
        createSuperAdmin("superadmin-409@example.com", "google-superadmin-409-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth(
                "google-superadmin-409-sub", "superadmin-409@example.com");

        // Pré-condition : un user existant avec un workspace (compte productif).
        User existingProspect = new User();
        existingProspect.setEmail("productif@example.com");
        existingProspect.setStatus("ACTIVE");
        userRepository.save(existingProspect);

        Workspace existingWs = new Workspace();
        existingWs.setName("PRODUCTIF-WS");
        existingWs.setSlug("productif-ws-" + System.currentTimeMillis());
        existingWs.setOwner(existingProspect);
        existingWs.setPlanCode("FREE");
        existingWs.setStatus("ACTIVE");
        existingWs.setLegalDomain("DROIT_DU_TRAVAIL");
        workspaceRepository.save(existingWs);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(existingWs);
        member.setUser(existingProspect);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        mockMvc.perform(post(URL)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("productif@example.com", "printemps2026", "AUTRE-WS")))
                .andExpect(status().isConflict());
    }

    // IT-05 : password < 8 caractères → 400 (Bean Validation)
    @Test
    void bootstrap_passwordTropCourt_jette400() throws Exception {
        createSuperAdmin("superadmin-400-pwd@example.com", "google-superadmin-400-pwd-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth(
                "google-superadmin-400-pwd-sub", "superadmin-400-pwd@example.com");

        mockMvc.perform(post(URL)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("prospect-400-pwd@example.com", "court", "PROSPECT-400-PWD")))
                .andExpect(status().isBadRequest());
    }

    // IT-06 : email invalide → 400
    @Test
    void bootstrap_emailInvalide_jette400() throws Exception {
        createSuperAdmin("superadmin-400-email@example.com", "google-superadmin-400-email-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth(
                "google-superadmin-400-email-sub", "superadmin-400-email@example.com");

        mockMvc.perform(post(URL)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("pas-un-email", "printemps2026", "PROSPECT-400-EMAIL")))
                .andExpect(status().isBadRequest());
    }

    // IT-07 : country inconnu → 400
    @Test
    void bootstrap_paysInconnu_jette400() throws Exception {
        createSuperAdmin("superadmin-400-country@example.com", "google-superadmin-400-country-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth(
                "google-superadmin-400-country-sub", "superadmin-400-country@example.com");

        String body = """
                {
                  "firstName": "Marjolaine",
                  "lastName": "RENVERSEZ",
                  "email": "prospect-400-country@example.com",
                  "password": "printemps2026",
                  "country": "ESPAGNE",
                  "legalDomain": "DROIT_DU_TRAVAIL",
                  "workspaceName": "PROSPECT-400-COUNTRY"
                }
                """;

        mockMvc.perform(post(URL)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // IT-08 : legalDomain inconnu → 400
    @Test
    void bootstrap_domaineInconnu_jette400() throws Exception {
        createSuperAdmin("superadmin-400-domain@example.com", "google-superadmin-400-domain-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth(
                "google-superadmin-400-domain-sub", "superadmin-400-domain@example.com");

        String body = """
                {
                  "firstName": "Marjolaine",
                  "lastName": "RENVERSEZ",
                  "email": "prospect-400-domain@example.com",
                  "password": "printemps2026",
                  "country": "FRANCE",
                  "legalDomain": "DROIT_FISCAL",
                  "workspaceName": "PROSPECT-400-DOMAIN"
                }
                """;

        mockMvc.perform(post(URL)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // IT-09 : workspaceName vide → 400
    @Test
    void bootstrap_workspaceNameVide_jette400() throws Exception {
        createSuperAdmin("superadmin-400-ws@example.com", "google-superadmin-400-ws-sub");
        OAuth2AuthenticationToken auth = buildGoogleAuth(
                "google-superadmin-400-ws-sub", "superadmin-400-ws@example.com");

        mockMvc.perform(post(URL)
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadJson("prospect-400-ws@example.com", "printemps2026", "")))
                .andExpect(status().isBadRequest());
    }
}
