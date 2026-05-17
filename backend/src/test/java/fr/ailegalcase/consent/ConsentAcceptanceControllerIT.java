package fr.ailegalcase.consent;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-240 SF-240-01 — Tests d'intégration de {@link ConsentAcceptanceController}.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class ConsentAcceptanceControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private UserConsentAcceptanceRepository acceptanceRepository;

    private OAuth2AuthenticationToken authWithWorkspace;
    private OAuth2AuthenticationToken authWithoutWorkspace;

    @BeforeEach
    void setUp() {
        acceptanceRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        // Utilisateur AVEC workspace primary
        User user = new User();
        user.setEmail("consent-it@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-consent-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("consent-it@example.com");
        workspace.setSlug("consent-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        authWithWorkspace = buildGoogleAuth("google-consent-sub", "consent-it@example.com");

        // Utilisateur SANS workspace (sign-up tout neuf)
        User freshUser = new User();
        freshUser.setEmail("fresh-consent-it@example.com");
        freshUser.setStatus("ACTIVE");
        userRepository.save(freshUser);

        AuthAccount freshAccount = new AuthAccount();
        freshAccount.setUser(freshUser);
        freshAccount.setProvider("GOOGLE");
        freshAccount.setProviderUserId("google-fresh-consent-sub");
        authAccountRepository.save(freshAccount);

        authWithoutWorkspace = buildGoogleAuth("google-fresh-consent-sub", "fresh-consent-it@example.com");
    }

    // IT-01
    @Test
    void accept_validBody_returns201AndPersistsTwoEntries() throws Exception {
        mockMvc.perform(post("/api/v1/consent/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentTypes\":[\"SIGNUP_TERMS\",\"PRIVACY_POLICY\"],\"version\":\"2026-05-11\"}")
                        .with(authentication(authWithWorkspace)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acceptances").isArray())
                .andExpect(jsonPath("$.acceptances.length()").value(2))
                .andExpect(jsonPath("$.acceptances[0].consentType").value("SIGNUP_TERMS"))
                .andExpect(jsonPath("$.acceptances[1].consentType").value("PRIVACY_POLICY"));

        assertThat(acceptanceRepository.count()).isEqualTo(2);
    }

    // IT-02
    @Test
    void accept_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/consent/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentTypes\":[\"SIGNUP_TERMS\"],\"version\":\"2026-05-11\"}"))
                .andExpect(status().isUnauthorized());
    }

    // IT-03
    @Test
    void accept_unknownConsentType_returns400WithExplicitMessage() throws Exception {
        mockMvc.perform(post("/api/v1/consent/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentTypes\":[\"UNKNOWN_TYPE\"],\"version\":\"2026-05-11\"}")
                        .with(authentication(authWithWorkspace)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Allowed")));

        assertThat(acceptanceRepository.count()).isZero();
    }

    // IT-04
    @Test
    void accept_emptyConsentTypes_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/consent/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentTypes\":[],\"version\":\"2026-05-11\"}")
                        .with(authentication(authWithWorkspace)))
                .andExpect(status().isBadRequest());
    }

    // IT-05
    @Test
    void accept_missingVersion_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/consent/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentTypes\":[\"SIGNUP_TERMS\"]}")
                        .with(authentication(authWithWorkspace)))
                .andExpect(status().isBadRequest());
    }

    // IT-06
    @Test
    void accept_calledTwice_persistsDistinctRows() throws Exception {
        String body = "{\"consentTypes\":[\"SIGNUP_TERMS\"],\"version\":\"2026-05-11\"}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/consent/accept")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body)
                            .with(authentication(authWithWorkspace)))
                    .andExpect(status().isCreated());
        }
        assertThat(acceptanceRepository.count()).isEqualTo(2);
    }

    // IT-07
    @Test
    void accept_userWithPrimaryWorkspace_responseHasWorkspaceId() throws Exception {
        mockMvc.perform(post("/api/v1/consent/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentTypes\":[\"PAYMENT_TERMS\"],\"version\":\"2026-05-11\"}")
                        .with(authentication(authWithWorkspace)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acceptances[0].workspaceId").isNotEmpty());
    }

    // IT-08
    @Test
    void accept_userWithoutWorkspace_responseWorkspaceIdIsNull() throws Exception {
        mockMvc.perform(post("/api/v1/consent/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consentTypes\":[\"SIGNUP_TERMS\"],\"version\":\"2026-05-11\"}")
                        .with(authentication(authWithoutWorkspace)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acceptances[0].workspaceId").value(org.hamcrest.Matchers.nullValue()));
    }

    private OAuth2AuthenticationToken buildGoogleAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
