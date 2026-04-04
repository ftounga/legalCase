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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ReferentialReportControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired LegalReferentialRepository referentialRepository;
    @Autowired ReferentialReportRepository reportRepository;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken ownerAuth;
    private OAuth2AuthenticationToken memberAuth;
    private LegalReferential systemEntry;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
        long ts = System.nanoTime();

        User owner = new User();
        owner.setEmail("owner-rpt-it-" + ts + "@example.com");
        owner.setStatus("ACTIVE");
        owner = userRepository.save(owner);

        AuthAccount ownerAcc = new AuthAccount();
        ownerAcc.setUser(owner);
        ownerAcc.setProvider("GOOGLE");
        ownerAcc.setProviderUserId("google-rpt-owner-" + ts);
        authAccountRepository.save(ownerAcc);

        Workspace ws = new Workspace();
        ws.setName("RPT IT WS " + ts);
        ws.setSlug("rpt-it-ws-" + ts);
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
        member.setEmail("member-rpt-it-" + ts + "@example.com");
        member.setStatus("ACTIVE");
        member = userRepository.save(member);

        AuthAccount memberAcc = new AuthAccount();
        memberAcc.setUser(member);
        memberAcc.setProvider("GOOGLE");
        memberAcc.setProviderUserId("google-rpt-member-" + ts);
        authAccountRepository.save(memberAcc);

        WorkspaceMember memberWs = new WorkspaceMember();
        memberWs.setWorkspace(ws);
        memberWs.setUser(member);
        memberWs.setMemberRole("MEMBER");
        memberWs.setPrimary(true);
        workspaceMemberRepository.save(memberWs);

        ownerAuth = buildGoogleAuth("google-rpt-owner-" + ts, "owner-rpt-it-" + ts + "@example.com");
        memberAuth = buildGoogleAuth("google-rpt-member-" + ts, "member-rpt-it-" + ts + "@example.com");

        systemEntry = new LegalReferential();
        systemEntry.setLegalDomain("DROIT_DU_TRAVAIL");
        systemEntry.setReferentialType("BAREME_MACRON");
        systemEntry.setEntryKey("RPT_TEST_" + ts);
        systemEntry.setLabel("Signalement test");
        systemEntry.setValueJson("{\"value\":1000}");
        systemEntry.setSystem(true);
        systemEntry.setActive(true);
        systemEntry = referentialRepository.save(systemEntry);
    }

    // IT-RPT-01 : OWNER → 403
    @Test
    void POST_report_owner_retourne_403() throws Exception {
        mockMvc.perform(post("/api/v1/referentials/" + systemEntry.getId() + "/reports")
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Valeur incorrecte\"}"))
                .andExpect(status().isForbidden());
    }

    // IT-RPT-02 : MEMBER → 201
    @Test
    void POST_report_member_retourne_201() throws Exception {
        mockMvc.perform(post("/api/v1/referentials/" + systemEntry.getId() + "/reports")
                        .with(authentication(memberAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"La valeur semble obsolète.\"}"))
                .andExpect(status().isCreated());
    }

    // IT-RPT-03 : doublon MEMBER → 409
    @Test
    void POST_report_member_doublon_retourne_409() throws Exception {
        String body = "{\"comment\":\"Déjà signalé.\"}";
        mockMvc.perform(post("/api/v1/referentials/" + systemEntry.getId() + "/reports")
                        .with(authentication(memberAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/referentials/" + systemEntry.getId() + "/reports")
                        .with(authentication(memberAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    // IT-RPT-04 : commentaire vide → 400
    @Test
    void POST_report_commentaire_vide_retourne_400() throws Exception {
        mockMvc.perform(post("/api/v1/referentials/" + systemEntry.getId() + "/reports")
                        .with(authentication(memberAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"\"}"))
                .andExpect(status().isBadRequest());
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
