package fr.ailegalcase.workspace;

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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
        "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
@AutoConfigureMockMvc
class WorkspaceControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;

    // I-01 : GET /api/v1/workspaces/current sans auth → 401
    @Test
    void current_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/current").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // I-02 : GET /api/v1/workspaces/current avec session → 200 avec les bons champs
    @Test
    void current_withSession_returns200() throws Exception {
        User user = new User();
        user.setEmail("workspace-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-ws-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("workspace-test@example.com");
        workspace.setSlug("test-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-ws-sub", "workspace-test@example.com");

        mockMvc.perform(get("/api/v1/workspaces/current")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("workspace-test@example.com"))
                .andExpect(jsonPath("$.planCode").value("STARTER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    // I-03 : POST /api/v1/workspaces → 201 avec nom valide
    @Test
    void createWorkspace_withValidName_returns201() throws Exception {
        User user = new User();
        user.setEmail("newuser@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-new-sub");
        authAccountRepository.save(account);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-new-sub", "newuser@example.com");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Cabinet Martin\"}")
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Cabinet Martin"))
                .andExpect(jsonPath("$.planCode").value("FREE"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    // I-04 : POST /api/v1/workspaces → 400 avec nom vide
    @Test
    void createWorkspace_withBlankName_returns400() throws Exception {
        User user = new User();
        user.setEmail("blank@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-blank-sub");
        authAccountRepository.save(account);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-blank-sub", "blank@example.com");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-05 : POST /api/v1/workspaces → 400 avec nom > 100 caractères
    @Test
    void createWorkspace_withTooLongName_returns400() throws Exception {
        User user = new User();
        user.setEmail("toolong@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-toolong-sub");
        authAccountRepository.save(account);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-toolong-sub", "toolong@example.com");
        String longName = "A".repeat(101);

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + longName + "\"}")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-06 : POST /api/v1/workspaces → 409 si workspace déjà existant
    @Test
    void createWorkspace_whenAlreadyExists_returns409() throws Exception {
        User user = new User();
        user.setEmail("existing@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-existing-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("Existing Workspace");
        workspace.setSlug("existing-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setPlanCode("FREE");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-existing-sub", "existing@example.com");

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Nouveau Workspace\"}")
                        .with(authentication(auth)))
                .andExpect(status().isConflict());
    }

    // I-07 : GET /api/v1/workspaces → liste les workspaces de l'utilisateur avec primary correct
    @Test
    void listWorkspaces_returnsAllUserWorkspaces() throws Exception {
        User user = new User();
        user.setEmail("list-ws@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-list-sub");
        authAccountRepository.save(account);

        Workspace ws1 = new Workspace();
        ws1.setName("Workspace Principal");
        ws1.setSlug("slug-list-1-" + System.currentTimeMillis());
        ws1.setOwner(user);
        ws1.setPlanCode("FREE");
        ws1.setStatus("ACTIVE");
        workspaceRepository.save(ws1);

        WorkspaceMember m1 = new WorkspaceMember();
        m1.setWorkspace(ws1);
        m1.setUser(user);
        m1.setMemberRole("OWNER");
        m1.setPrimary(true);
        workspaceMemberRepository.save(m1);

        Workspace ws2 = new Workspace();
        ws2.setName("Workspace Secondaire");
        ws2.setSlug("slug-list-2-" + System.currentTimeMillis());
        ws2.setOwner(user);
        ws2.setPlanCode("FREE");
        ws2.setStatus("ACTIVE");
        workspaceRepository.save(ws2);

        WorkspaceMember m2 = new WorkspaceMember();
        m2.setWorkspace(ws2);
        m2.setUser(user);
        m2.setMemberRole("MEMBER");
        m2.setPrimary(false);
        workspaceMemberRepository.save(m2);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-list-sub", "list-ws@example.com");

        mockMvc.perform(get("/api/v1/workspaces")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.name == 'Workspace Principal')].primary").value(true))
                .andExpect(jsonPath("$[?(@.name == 'Workspace Secondaire')].primary").value(false));
    }

    // I-08 : POST /api/v1/workspaces/{id}/switch → 200, is_primary basculé
    @Test
    void switchWorkspace_validTarget_switchesPrimary() throws Exception {
        User user = new User();
        user.setEmail("switch@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-switch-sub");
        authAccountRepository.save(account);

        Workspace ws1 = new Workspace();
        ws1.setName("WS Primary");
        ws1.setSlug("slug-sw1-" + System.currentTimeMillis());
        ws1.setOwner(user);
        ws1.setPlanCode("FREE");
        ws1.setStatus("ACTIVE");
        workspaceRepository.save(ws1);

        WorkspaceMember m1 = new WorkspaceMember();
        m1.setWorkspace(ws1);
        m1.setUser(user);
        m1.setMemberRole("OWNER");
        m1.setPrimary(true);
        workspaceMemberRepository.save(m1);

        Workspace ws2 = new Workspace();
        ws2.setName("WS Secondaire");
        ws2.setSlug("slug-sw2-" + System.currentTimeMillis());
        ws2.setOwner(user);
        ws2.setPlanCode("FREE");
        ws2.setStatus("ACTIVE");
        workspaceRepository.save(ws2);

        WorkspaceMember m2 = new WorkspaceMember();
        m2.setWorkspace(ws2);
        m2.setUser(user);
        m2.setMemberRole("MEMBER");
        m2.setPrimary(false);
        workspaceMemberRepository.save(m2);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-switch-sub", "switch@example.com");

        mockMvc.perform(post("/api/v1/workspaces/" + ws2.getId() + "/switch")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("WS Secondaire"))
                .andExpect(jsonPath("$.primary").value(true));
    }

    // I-09 : POST /api/v1/workspaces/{id}/switch → 403 si non membre
    @Test
    void switchWorkspace_notMember_returns403() throws Exception {
        User user = new User();
        user.setEmail("notmember@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-notmember-sub");
        authAccountRepository.save(account);

        Workspace ownWs = new Workspace();
        ownWs.setName("Own WS");
        ownWs.setSlug("slug-own-" + System.currentTimeMillis());
        ownWs.setOwner(user);
        ownWs.setPlanCode("FREE");
        ownWs.setStatus("ACTIVE");
        workspaceRepository.save(ownWs);

        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ownWs);
        m.setUser(user);
        m.setMemberRole("OWNER");
        m.setPrimary(true);
        workspaceMemberRepository.save(m);

        // Workspace d'un autre user
        User otherUser = new User();
        otherUser.setEmail("other@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        Workspace otherWs = new Workspace();
        otherWs.setName("Other WS");
        otherWs.setSlug("slug-other-" + System.currentTimeMillis());
        otherWs.setOwner(otherUser);
        otherWs.setPlanCode("FREE");
        otherWs.setStatus("ACTIVE");
        workspaceRepository.save(otherWs);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-notmember-sub", "notmember@example.com");

        mockMvc.perform(post("/api/v1/workspaces/" + otherWs.getId() + "/switch")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // I-10 : POST /api/v1/workspaces/{id}/switch → 403 si workspace inexistant
    @Test
    void switchWorkspace_unknownWorkspace_returns403() throws Exception {
        User user = new User();
        user.setEmail("unknown-ws@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-unknown-sub");
        authAccountRepository.save(account);

        Workspace ownWs = new Workspace();
        ownWs.setName("Own WS");
        ownWs.setSlug("slug-unk-" + System.currentTimeMillis());
        ownWs.setOwner(user);
        ownWs.setPlanCode("FREE");
        ownWs.setStatus("ACTIVE");
        workspaceRepository.save(ownWs);

        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ownWs);
        m.setUser(user);
        m.setMemberRole("OWNER");
        m.setPrimary(true);
        workspaceMemberRepository.save(m);

        OAuth2AuthenticationToken auth = buildGoogleAuth("google-unknown-sub", "unknown-ws@example.com");

        mockMvc.perform(post("/api/v1/workspaces/" + UUID.randomUUID() + "/switch")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
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
}
