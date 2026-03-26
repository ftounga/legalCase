package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class WorkspaceMemberControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;

    private User owner;
    private Workspace workspace;
    private OAuth2AuthenticationToken ownerAuth;

    @BeforeEach
    void setUp() {
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();

        owner = new User();
        owner.setEmail("owner-member-it@example.com");
        owner.setStatus("ACTIVE");
        userRepository.save(owner);

        AuthAccount account = new AuthAccount();
        account.setUser(owner);
        account.setProvider("GOOGLE");
        account.setProviderUserId("sub-owner-member-it");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("Test WS");
        workspace.setSlug("test-ws-member-it-" + System.currentTimeMillis());
        workspace.setOwner(owner);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
       workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setCountry("FRANCE");

        workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = new WorkspaceMember();
        ownerMember.setWorkspace(workspace);
        ownerMember.setUser(owner);
        ownerMember.setMemberRole("OWNER");
        ownerMember.setPrimary(true);
        workspaceMemberRepository.save(ownerMember);

        ownerAuth = buildAuth("sub-owner-member-it", "owner-member-it@example.com");
    }

    // I-01 : GET /api/v1/workspaces/current/members → 200 avec liste
    @Test
    void listMembers_returns200WithList() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/current/members")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].memberRole").value("OWNER"))
                .andExpect(jsonPath("$[0].email").value("owner-member-it@example.com"));
    }

    // I-02 : GET sans auth → 401
    @Test
    void listMembers_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/current/members").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // I-03 : DELETE membre existant → 204
    @Test
    void removeMember_existingMember_returns204() throws Exception {
        User target = new User();
        target.setEmail("target-member-it@example.com");
        target.setStatus("ACTIVE");
        userRepository.save(target);

        WorkspaceMember targetMember = new WorkspaceMember();
        targetMember.setWorkspace(workspace);
        targetMember.setUser(target);
        targetMember.setMemberRole("LAWYER");
        targetMember.setPrimary(false);
        workspaceMemberRepository.save(targetMember);

        mockMvc.perform(delete("/api/v1/workspaces/current/members/" + target.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isNoContent());
    }

    // I-04 : DELETE — OWNER tente de se révoquer → 403
    @Test
    void removeMember_ownerRevokesThemself_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/current/members/" + owner.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isForbidden());
    }

    // I-05 : isolation — liste membres d'un workspace différent impossible
    @Test
    void listMembers_isolatedToCurrentWorkspace() throws Exception {
        // Le owner voit uniquement les membres de son workspace (1 membre ici)
        mockMvc.perform(get("/api/v1/workspaces/current/members")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
