package fr.ailegalcase.referential;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ReferentialUpdateControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired LegalReferentialRepository referentialRepository;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken ownerAuth;
    private OAuth2AuthenticationToken memberAuth;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // OWNER
        User owner = new User();
        owner.setEmail("owner-upd-it-" + ts + "@example.com");
        owner.setStatus("ACTIVE");
        owner = userRepository.save(owner);

        AuthAccount ownerAccount = new AuthAccount();
        ownerAccount.setUser(owner);
        ownerAccount.setProvider("GOOGLE");
        ownerAccount.setProviderUserId("google-upd-owner-" + ts);
        authAccountRepository.save(ownerAccount);

        Workspace ws = new Workspace();
        ws.setName("UPD IT WS");
        ws.setSlug("upd-it-ws-" + System.currentTimeMillis());
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

        // MEMBER
        User member = new User();
        member.setEmail("member-upd-it-" + ts + "@example.com");
        member.setStatus("ACTIVE");
        member = userRepository.save(member);

        AuthAccount memberAccount = new AuthAccount();
        memberAccount.setUser(member);
        memberAccount.setProvider("GOOGLE");
        memberAccount.setProviderUserId("google-upd-member-" + ts);
        authAccountRepository.save(memberAccount);

        WorkspaceMember memberWs = new WorkspaceMember();
        memberWs.setWorkspace(ws);
        memberWs.setUser(member);
        memberWs.setMemberRole("MEMBER");
        memberWs.setPrimary(true);
        workspaceMemberRepository.save(memberWs);

        ownerAuth = buildGoogleAuth("google-upd-owner-" + ts, "owner-upd-it-" + ts + "@example.com");
        memberAuth = buildGoogleAuth("google-upd-member-" + ts, "member-upd-it-" + ts + "@example.com");

        // Default: Anthropic returns VALID
        when(anthropicService.analyzeFast(any(AiCallContext.class), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("VALID", "haiku", 10, 5));
    }

    // IT-PUT-01 : MEMBER → 403
    @Test
    void PUT_referential_member_retourne_403() throws Exception {
        LegalReferential entry = buildSystemEntry("LITIGATION_TYPE", "DISCRIM2");
        entry = referentialRepository.save(entry);

        mockMvc.perform(put("/api/v1/referentials/" + entry.getId())
                        .with(authentication(memberAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Test\",\"valueJson\":\"{}\",\"force\":false}"))
                .andExpect(status().isForbidden());
    }

    // IT-PUT-02 : OWNER + Anthropic VALID → 200 saved=true
    @Test
    void PUT_referential_owner_valide_retourne_200_saved_true() throws Exception {
        LegalReferential entry = buildSystemEntry("LITIGATION_TYPE", "DISCRIM3");
        entry = referentialRepository.save(entry);

        mockMvc.perform(put("/api/v1/referentials/" + entry.getId())
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Discrimination\",\"valueJson\":\"{\\\"years\\\":4}\",\"force\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved", is(true)));
    }

    // IT-PUT-03 : force=true → 200 saved=true sans appel Anthropic
    @Test
    void PUT_referential_force_true_sauvegarde_sans_appel_anthropic() throws Exception {
        LegalReferential entry = buildSystemEntry("BAREME_MACRON", "LICENCIE2");
        entry = referentialRepository.save(entry);

        mockMvc.perform(put("/api/v1/referentials/" + entry.getId())
                        .with(authentication(ownerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Licenciement\",\"valueJson\":\"{\\\"supported\\\":false}\",\"force\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved", is(true)));

        verify(anthropicService, never()).analyzeFast(any(AiCallContext.class), any(), any(), anyInt());
    }

    private LegalReferential buildSystemEntry(String type, String key) {
        LegalReferential e = new LegalReferential();
        e.setLegalDomain("DROIT_DU_TRAVAIL");
        e.setReferentialType(type);
        e.setEntryKey(key);
        e.setLabel("Test entry " + key);
        e.setValueJson("{\"years\":5}");
        e.setSystem(true);
        e.setActive(true);
        return e;
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
