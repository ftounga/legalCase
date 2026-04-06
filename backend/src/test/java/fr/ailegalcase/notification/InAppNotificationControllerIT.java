package fr.ailegalcase.notification;

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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class InAppNotificationControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired InAppNotificationRepository notificationRepository;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken userAAuth;
    private OAuth2AuthenticationToken userBAuth;
    private UUID userAId;
    private UUID workspaceAId;
    private UUID userBId;
    private UUID workspaceBId;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // User A + Workspace A
        User userA = new User();
        userA.setEmail("notif-a-" + ts + "@example.com");
        userA.setStatus("ACTIVE");
        userA = userRepository.save(userA);
        userAId = userA.getId();

        AuthAccount accA = new AuthAccount();
        accA.setUser(userA);
        accA.setProvider("GOOGLE");
        accA.setProviderUserId("google-notif-a-" + ts);
        authAccountRepository.save(accA);

        Workspace wsA = new Workspace();
        wsA.setName("WS Notif A " + ts);
        wsA.setSlug("ws-notif-a-" + ts);
        wsA.setOwner(userA);
        wsA.setLegalDomain("DROIT_DU_TRAVAIL");
        wsA.setPlanCode("STARTER");
        wsA.setStatus("ACTIVE");
        wsA = workspaceRepository.save(wsA);
        workspaceAId = wsA.getId();

        WorkspaceMember memberA = new WorkspaceMember();
        memberA.setWorkspace(wsA);
        memberA.setUser(userA);
        memberA.setMemberRole("OWNER");
        memberA.setPrimary(true);
        workspaceMemberRepository.save(memberA);

        userAAuth = buildGoogleAuth("google-notif-a-" + ts, "notif-a-" + ts + "@example.com");

        // User B + Workspace B (isolation)
        User userB = new User();
        userB.setEmail("notif-b-" + ts + "@example.com");
        userB.setStatus("ACTIVE");
        userB = userRepository.save(userB);
        userBId = userB.getId();

        AuthAccount accB = new AuthAccount();
        accB.setUser(userB);
        accB.setProvider("GOOGLE");
        accB.setProviderUserId("google-notif-b-" + ts);
        authAccountRepository.save(accB);

        Workspace wsB = new Workspace();
        wsB.setName("WS Notif B " + ts);
        wsB.setSlug("ws-notif-b-" + ts);
        wsB.setOwner(userB);
        wsB.setLegalDomain("DROIT_DU_TRAVAIL");
        wsB.setPlanCode("STARTER");
        wsB.setStatus("ACTIVE");
        wsB = workspaceRepository.save(wsB);
        workspaceBId = wsB.getId();

        WorkspaceMember memberB = new WorkspaceMember();
        memberB.setWorkspace(wsB);
        memberB.setUser(userB);
        memberB.setMemberRole("OWNER");
        memberB.setPrimary(true);
        workspaceMemberRepository.save(memberB);

        userBAuth = buildGoogleAuth("google-notif-b-" + ts, "notif-b-" + ts + "@example.com");
    }

    @Test
    void GET_notifications_retourne_200_avec_liste_paginee() throws Exception {
        createNotification(userAId, workspaceAId, NotificationType.ANALYSIS_DONE,
                "Analyse terminée", "Votre dossier a été analysé", "/case-files/123/synthesis");

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("ANALYSIS_DONE"))
                .andExpect(jsonPath("$.content[0].title").value("Analyse terminée"))
                .andExpect(jsonPath("$.content[0].isRead").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void GET_notifications_isolation_workspace() throws Exception {
        createNotification(userAId, workspaceAId, NotificationType.ANALYSIS_DONE,
                "Notif workspace A", null, null);
        createNotification(userBId, workspaceBId, NotificationType.ANALYSIS_DONE,
                "Notif workspace B", null, null);

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Notif workspace A"));

        mockMvc.perform(get("/api/v1/notifications")
                        .with(authentication(userBAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Notif workspace B"));
    }

    @Test
    void GET_unread_count_retourne_200() throws Exception {
        createNotification(userAId, workspaceAId, NotificationType.ANALYSIS_DONE, "N1", null, null);
        createNotification(userAId, workspaceAId, NotificationType.DEADLINE_APPROACHING, "N2", null, null);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void PATCH_read_marque_notification_lue() throws Exception {
        InAppNotification notif = createNotification(userAId, workspaceAId,
                NotificationType.ANALYSIS_DONE, "À lire", null, null);

        mockMvc.perform(patch("/api/v1/notifications/" + notif.getId() + "/read")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(userAAuth)))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void PATCH_read_retourne_404_si_notification_inexistante() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/" + UUID.randomUUID() + "/read")
                        .with(authentication(userAAuth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void PATCH_read_retourne_403_si_notification_autre_workspace() throws Exception {
        InAppNotification notifB = createNotification(userBId, workspaceBId,
                NotificationType.ANALYSIS_DONE, "Notif B", null, null);

        mockMvc.perform(patch("/api/v1/notifications/" + notifB.getId() + "/read")
                        .with(authentication(userAAuth)))
                .andExpect(status().isNotFound());
    }

    @Test
    void PATCH_read_all_marque_toutes_les_notifications_lues() throws Exception {
        createNotification(userAId, workspaceAId, NotificationType.ANALYSIS_DONE, "N1", null, null);
        createNotification(userAId, workspaceAId, NotificationType.DEADLINE_APPROACHING, "N2", null, null);

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .with(authentication(userAAuth)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .with(authentication(userAAuth)))
                .andExpect(jsonPath("$.count").value(0));
    }

    private InAppNotification createNotification(UUID userId, UUID workspaceId,
                                                  NotificationType type, String title,
                                                  String message, String link) {
        InAppNotification n = new InAppNotification();
        n.setUserId(userId);
        n.setWorkspaceId(workspaceId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setLink(link);
        return notificationRepository.save(n);
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
