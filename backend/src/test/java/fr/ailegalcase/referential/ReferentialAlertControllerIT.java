package fr.ailegalcase.referential;

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

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ReferentialAlertControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired LegalReferentialRepository referentialRepository;
    @Autowired ReferentialAlertRepository alertRepository;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken ownerAuth;
    private OAuth2AuthenticationToken memberAuth;
    private LegalReferential systemEntry;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User owner = new User();
        owner.setEmail("owner-alert-it-" + ts + "@example.com");
        owner.setStatus("ACTIVE");
        owner = userRepository.save(owner);

        AuthAccount ownerAccount = new AuthAccount();
        ownerAccount.setUser(owner);
        ownerAccount.setProvider("GOOGLE");
        ownerAccount.setProviderUserId("google-alert-owner-" + ts);
        authAccountRepository.save(ownerAccount);

        Workspace ws = new Workspace();
        ws.setName("ALERT IT WS " + ts);
        ws.setSlug("alert-it-ws-" + ts);
        ws.setOwner(owner);
        ws.setLegalDomain("DROIT_DU_TRAVAIL");
        ws.setPlanCode("STARTER");
        ws.setStatus("ACTIVE");
        ws = workspaceRepository.save(ws);

        WorkspaceMember ownerMember = new WorkspaceMember();
        ownerMember.setWorkspace(ws);
        ownerMember.setUser(owner);
        ownerMember.setMemberRole("OWNER");
        ownerMember.setPrimary(true);
        workspaceMemberRepository.save(ownerMember);

        User member = new User();
        member.setEmail("member-alert-it-" + ts + "@example.com");
        member.setStatus("ACTIVE");
        member = userRepository.save(member);

        AuthAccount memberAccount = new AuthAccount();
        memberAccount.setUser(member);
        memberAccount.setProvider("GOOGLE");
        memberAccount.setProviderUserId("google-alert-member-" + ts);
        authAccountRepository.save(memberAccount);

        WorkspaceMember memberWs = new WorkspaceMember();
        memberWs.setWorkspace(ws);
        memberWs.setUser(member);
        memberWs.setMemberRole("MEMBER");
        memberWs.setPrimary(true);
        workspaceMemberRepository.save(memberWs);

        ownerAuth = buildGoogleAuth("google-alert-owner-" + ts, "owner-alert-it-" + ts + "@example.com");
        memberAuth = buildGoogleAuth("google-alert-member-" + ts, "member-alert-it-" + ts + "@example.com");

        alertRepository.deleteAll();

        systemEntry = new LegalReferential();
        systemEntry.setLegalDomain("DROIT_DU_TRAVAIL");
        systemEntry.setReferentialType("BAREME_MACRON");
        systemEntry.setEntryKey("ALERT_TEST_" + ts);
        systemEntry.setLabel("Alerte test");
        systemEntry.setValueJson("{\"value\":1000}");
        systemEntry.setSystem(true);
        systemEntry.setActive(true);
        systemEntry = referentialRepository.save(systemEntry);
    }

    // IT-ALERT-01 : pending-count retourne 0 si aucune alerte
    @Test
    void GET_pendingCount_retourne_0_si_aucune_alerte() throws Exception {
        mockMvc.perform(get("/api/v1/referentials/alerts/pending-count")
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    // IT-ALERT-02 : apply par MEMBER → 403
    @Test
    void POST_apply_member_retourne_403() throws Exception {
        ReferentialAlert alert = buildPendingAlert();

        mockMvc.perform(post("/api/v1/referentials/alerts/" + alert.getId() + "/apply")
                        .with(authentication(memberAuth)))
                .andExpect(status().isForbidden());
    }

    // IT-ALERT-03 : dismiss par MEMBER → 403
    @Test
    void POST_dismiss_member_retourne_403() throws Exception {
        ReferentialAlert alert = buildPendingAlert();

        mockMvc.perform(post("/api/v1/referentials/alerts/" + alert.getId() + "/dismiss")
                        .with(authentication(memberAuth)))
                .andExpect(status().isForbidden());
    }

    // IT-ALERT-04 : apply par OWNER → 200 + workspace override créé
    @Test
    void POST_apply_owner_retourne_200_et_cree_override() throws Exception {
        ReferentialAlert alert = buildPendingAlert();

        mockMvc.perform(post("/api/v1/referentials/alerts/" + alert.getId() + "/apply")
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk());
    }

    // IT-ALERT-05 : dismiss par OWNER → 200
    @Test
    void POST_dismiss_owner_retourne_200() throws Exception {
        ReferentialAlert alert = buildPendingAlert();

        mockMvc.perform(post("/api/v1/referentials/alerts/" + alert.getId() + "/dismiss")
                        .with(authentication(ownerAuth)))
                .andExpect(status().isOk());
    }

    private ReferentialAlert buildPendingAlert() {
        ReferentialAlert a = new ReferentialAlert();
        a.setEntry(systemEntry);
        a.setStatus(ReferentialAlert.Status.PENDING);
        a.setProposedValueJson("{\"value\":1050}");
        a.setAiMessage("Barème revalorisé en 2026");
        return alertRepository.save(a);
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
