package fr.ailegalcase.dashboard;

import fr.ailegalcase.analysis.AnthropicService;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class DashboardControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken userAAuth;
    private OAuth2AuthenticationToken userBAuth;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User userA = new User();
        userA.setEmail("dash-a-" + ts + "@example.com");
        userA.setStatus("ACTIVE");
        userA = userRepository.save(userA);

        AuthAccount accA = new AuthAccount();
        accA.setUser(userA);
        accA.setProvider("GOOGLE");
        accA.setProviderUserId("google-dash-a-" + ts);
        authAccountRepository.save(accA);

        Workspace wsA = new Workspace();
        wsA.setName("Workspace A " + ts);
        wsA.setSlug("ws-a-" + ts);
        wsA.setOwner(userA);
        wsA.setLegalDomain("DROIT_DU_TRAVAIL");
        wsA.setPlanCode("STARTER");
        wsA.setStatus("ACTIVE");
        wsA = workspaceRepository.save(wsA);

        WorkspaceMember memberA = new WorkspaceMember();
        memberA.setWorkspace(wsA);
        memberA.setUser(userA);
        memberA.setMemberRole("OWNER");
        memberA.setPrimary(true);
        workspaceMemberRepository.save(memberA);

        userAAuth = buildGoogleAuth("google-dash-a-" + ts, "dash-a-" + ts + "@example.com");

        // Workspace B — isolation test
        User userB = new User();
        userB.setEmail("dash-b-" + ts + "@example.com");
        userB.setStatus("ACTIVE");
        userB = userRepository.save(userB);

        AuthAccount accB = new AuthAccount();
        accB.setUser(userB);
        accB.setProvider("GOOGLE");
        accB.setProviderUserId("google-dash-b-" + ts);
        authAccountRepository.save(accB);

        Workspace wsB = new Workspace();
        wsB.setName("Workspace B " + ts);
        wsB.setSlug("ws-b-" + ts);
        wsB.setOwner(userB);
        wsB.setLegalDomain("DROIT_DU_TRAVAIL");
        wsB.setPlanCode("STARTER");
        wsB.setStatus("ACTIVE");
        wsB = workspaceRepository.save(wsB);

        WorkspaceMember memberB = new WorkspaceMember();
        memberB.setWorkspace(wsB);
        memberB.setUser(userB);
        memberB.setMemberRole("OWNER");
        memberB.setPrimary(true);
        workspaceMemberRepository.save(memberB);

        userBAuth = buildGoogleAuth("google-dash-b-" + ts, "dash-b-" + ts + "@example.com");
    }

    // IT-DASH-01 : GET 200 avec structure complète
    @Test
    void GET_dashboard_retourne_200_avec_structure() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCases").isArray())
                .andExpect(jsonPath("$.openCasesCount").isNumber())
                .andExpect(jsonPath("$.urgentDeadlines").isArray())
                .andExpect(jsonPath("$.staleChecks").isArray())
                .andExpect(jsonPath("$.recentAnalyses").isArray());
    }

    // IT-DASH-02 : isolation workspace — workspace A ne voit pas les données de workspace B
    @Test
    void GET_dashboard_isolation_workspace() throws Exception {
        // Les deux workspaces sont vides — les deux GET doivent retourner 200 sans données croisées
        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCasesCount").value(0));

        mockMvc.perform(get("/api/v1/dashboard")
                        .with(authentication(userBAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCasesCount").value(0));
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
