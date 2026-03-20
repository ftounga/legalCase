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
