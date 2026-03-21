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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class WorkspaceInvitationControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private WorkspaceInvitationRepository workspaceInvitationRepository;

    private User owner;
    private Workspace workspace;
    private OAuth2AuthenticationToken ownerAuth;

    @BeforeEach
    void setUp() {
        workspaceInvitationRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();

        owner = new User();
        owner.setEmail("owner-inv-it@example.com");
        owner.setStatus("ACTIVE");
        userRepository.save(owner);

        AuthAccount account = new AuthAccount();
        account.setUser(owner);
        account.setProvider("GOOGLE");
        account.setProviderUserId("sub-owner-inv-it");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("Inv WS");
        workspace.setSlug("inv-ws-it-" + System.currentTimeMillis());
        workspace.setOwner(owner);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
       workspace.setLegalDomain("DROIT_DU_TRAVAIL");

        workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = new WorkspaceMember();
        ownerMember.setWorkspace(workspace);
        ownerMember.setUser(owner);
        ownerMember.setMemberRole("OWNER");
        ownerMember.setPrimary(true);
        workspaceMemberRepository.save(ownerMember);

        ownerAuth = buildAuth("sub-owner-inv-it", "owner-inv-it@example.com");
    }

    // I-01 : POST invitation → 201
    @Test
    void createInvitation_validPayload_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/current/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invitee@example.com","role":"LAWYER"}
                                """)
                        .with(authentication(ownerAuth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("invitee@example.com"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    // I-02 : POST invitation — doublon PENDING → 409
    @Test
    void createInvitation_duplicatePending_returns409() throws Exception {
        WorkspaceInvitation existing = new WorkspaceInvitation();
        existing.setWorkspaceId(workspace.getId());
        existing.setInvitedByUserId(owner.getId());
        existing.setEmail("invitee@example.com");
        existing.setRole("LAWYER");
        existing.setToken("existing-token");
        existing.setStatus("PENDING");
        existing.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        workspaceInvitationRepository.save(existing);

        mockMvc.perform(post("/api/v1/workspaces/current/invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invitee@example.com","role":"LAWYER"}
                                """)
                        .with(authentication(ownerAuth)))
                .andExpect(status().isConflict());
    }

    // I-03 : GET invitations → 200 avec liste PENDING
    @Test
    void listInvitations_returns200() throws Exception {
        WorkspaceInvitation inv = new WorkspaceInvitation();
        inv.setWorkspaceId(workspace.getId());
        inv.setInvitedByUserId(owner.getId());
        inv.setEmail("someone@example.com");
        inv.setRole("ADMIN");
        inv.setToken("list-token");
        inv.setStatus("PENDING");
        inv.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        workspaceInvitationRepository.save(inv);

        mockMvc.perform(get("/api/v1/workspaces/current/invitations")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").value("someone@example.com"));
    }

    // I-04 : DELETE invitation PENDING → 204
    @Test
    void revokeInvitation_pendingInvitation_returns204() throws Exception {
        WorkspaceInvitation inv = new WorkspaceInvitation();
        inv.setWorkspaceId(workspace.getId());
        inv.setInvitedByUserId(owner.getId());
        inv.setEmail("revoke@example.com");
        inv.setRole("MEMBER");
        inv.setToken("revoke-token");
        inv.setStatus("PENDING");
        inv.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        workspaceInvitationRepository.save(inv);

        mockMvc.perform(delete("/api/v1/workspaces/current/invitations/" + inv.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isNoContent());
    }

    // I-05 : POST accept — token valide → 200
    @Test
    void acceptInvitation_validToken_returns200() throws Exception {
        WorkspaceInvitation inv = new WorkspaceInvitation();
        inv.setWorkspaceId(workspace.getId());
        inv.setInvitedByUserId(owner.getId());
        inv.setEmail("acceptee@example.com");
        inv.setRole("LAWYER");
        inv.setToken("accept-valid-token");
        inv.setStatus("PENDING");
        inv.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        workspaceInvitationRepository.save(inv);

        User acceptee = new User();
        acceptee.setEmail("acceptee@example.com");
        acceptee.setStatus("ACTIVE");
        userRepository.save(acceptee);

        AuthAccount accepteeAccount = new AuthAccount();
        accepteeAccount.setUser(acceptee);
        accepteeAccount.setProvider("GOOGLE");
        accepteeAccount.setProviderUserId("sub-acceptee");
        authAccountRepository.save(accepteeAccount);

        Workspace accepteeWorkspace = new Workspace();
        accepteeWorkspace.setName("Acceptee WS");
        accepteeWorkspace.setSlug("acceptee-ws-" + System.currentTimeMillis());
        accepteeWorkspace.setOwner(acceptee);
        accepteeWorkspace.setPlanCode("STARTER");
        accepteeWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(accepteeWorkspace);

        WorkspaceMember accepteeMember = new WorkspaceMember();
        accepteeMember.setWorkspace(accepteeWorkspace);
        accepteeMember.setUser(acceptee);
        accepteeMember.setMemberRole("OWNER");
        accepteeMember.setPrimary(true);
        workspaceMemberRepository.save(accepteeMember);

        OAuth2AuthenticationToken accepteeAuth = buildAuth("sub-acceptee", "acceptee@example.com");

        mockMvc.perform(post("/api/v1/workspace/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"accept-valid-token"}
                                """)
                        .with(authentication(accepteeAuth)))
                .andExpect(status().isOk());
    }

    // I-06 : POST accept — token expiré → 409
    @Test
    void acceptInvitation_expiredToken_returns409() throws Exception {
        WorkspaceInvitation inv = new WorkspaceInvitation();
        inv.setWorkspaceId(workspace.getId());
        inv.setInvitedByUserId(owner.getId());
        inv.setEmail("owner-inv-it@example.com");
        inv.setRole("LAWYER");
        inv.setToken("expired-accept-token");
        inv.setStatus("PENDING");
        inv.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        workspaceInvitationRepository.save(inv);

        mockMvc.perform(post("/api/v1/workspace/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"expired-accept-token"}
                                """)
                        .with(authentication(ownerAuth)))
                .andExpect(status().isConflict());
    }

    // I-07 : DELETE invitation d'un autre workspace → 404
    @Test
    void revokeInvitation_otherWorkspace_returns404() throws Exception {
        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("Other");
        otherWorkspace.setSlug("other-ws-inv-" + System.currentTimeMillis());
        otherWorkspace.setOwner(owner);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(otherWorkspace);

        WorkspaceInvitation inv = new WorkspaceInvitation();
        inv.setWorkspaceId(otherWorkspace.getId());
        inv.setInvitedByUserId(owner.getId());
        inv.setEmail("other@example.com");
        inv.setRole("LAWYER");
        inv.setToken("other-ws-token");
        inv.setStatus("PENDING");
        inv.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        workspaceInvitationRepository.save(inv);

        mockMvc.perform(delete("/api/v1/workspaces/current/invitations/" + inv.getId())
                        .with(authentication(ownerAuth)))
                .andExpect(status().isNotFound());
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
