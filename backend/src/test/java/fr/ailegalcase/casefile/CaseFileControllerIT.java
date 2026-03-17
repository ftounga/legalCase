package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.*;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
        "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
@AutoConfigureMockMvc
class CaseFileControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;

    private OAuth2AuthenticationToken auth;

    @BeforeEach
    void setUp() {
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("casefile-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-cf-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("casefile-test@example.com");
        workspace.setSlug("cf-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        workspaceMemberRepository.save(member);

        auth = buildGoogleAuth("google-cf-sub", "casefile-test@example.com");
    }

    // I-01 : POST avec payload valide → 201
    @Test
    void create_validPayload_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Licenciement Dupont","legalDomain":"EMPLOYMENT_LAW","description":"Test"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Licenciement Dupont"))
                .andExpect(jsonPath("$.legalDomain").value("EMPLOYMENT_LAW"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    // I-02 : title absent → 400
    @Test
    void create_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"legalDomain":"EMPLOYMENT_LAW"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title is required"));
    }

    // I-03 : legalDomain invalide → 400
    @Test
    void create_invalidLegalDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Test","legalDomain":"IMMIGRATION_LAW"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only EMPLOYMENT_LAW is supported in V1"));
    }

    // I-04 : sans auth → 401
    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Test","legalDomain":"EMPLOYMENT_LAW"}
                                """))
                .andExpect(status().isUnauthorized());
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
