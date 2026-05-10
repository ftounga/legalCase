package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SF-210-01 : IT minimal POST + GET + gate BE. */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class MediationFamilialePreSaisineControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFrFa;
    private OAuth2AuthenticationToken authBeFa;
    private CaseFile faFrCf;
    private CaseFile faBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        User uFr = save(new User(), u -> { u.setEmail("med-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-med-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRMD " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        faFrCf = saveCf(uFr, wsFr, "CFRMD " + ts, "DROIT_FAMILLE");
        authFrFa = buildAuth("g-med-fr-" + ts, "med-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("med-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-med-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEMD " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        faBeCf = saveCf(uBe, wsBe, "CBEMD " + ts, "DROIT_FAMILLE");
        authBeFa = buildAuth("g-med-be-" + ts, "med-be-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String motif, boolean tentee, String exception) {
        Map<String, Object> m = new HashMap<>();
        m.put("motifSaisine", motif);
        m.put("mediationTentee", tentee);
        m.put("dateMediation", null);
        m.put("exceptionApplicable", exception);
        m.put("exceptionDetail", null);
        return m;
    }

    @Test
    void POST_fr_motifInscope_sans_mediation_renvoie_irrecevable() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/mediation-familiale-pre-saisine")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("AUTORITE_PARENTALE", false, "AUCUNE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdict").value("IRRECEVABLE"))
                .andExpect(jsonPath("$.motifInScope").value(true));
    }

    @Test
    void POST_fr_avec_violence_renvoie_recevable() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/mediation-familiale-pre-saisine")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("AUTORITE_PARENTALE", false, "VIOLENCE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECEVABLE"))
                .andExpect(jsonPath("$.dispenseApplicable").value(true));
    }

    @Test
    void POST_fr_divorce_renvoie_non_concerne() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/mediation-familiale-pre-saisine")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("DIVORCE_CONTENTIEUX", false, "AUCUNE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONCERNE"));
    }

    @Test
    void POST_be_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/mediation-familiale-pre-saisine")
                        .with(authentication(authBeFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("AUTORITE_PARENTALE", false, "AUCUNE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/mediation-familiale-pre-saisine")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("CONTRIBUTION_ENTRETIEN", true, "AUCUNE"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/mediation-familiale-pre-saisine")
                        .with(authentication(authFrFa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECEVABLE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/mediation-familiale-pre-saisine")
                        .with(authentication(authFrFa)))
                .andExpect(status().isNotFound());
    }

    // helpers
    private User save(User u, java.util.function.Consumer<User> init) {
        init.accept(u);
        return userRepository.save(u);
    }
    private void saveAuth(User user, String providerUserId) {
        AuthAccount a = new AuthAccount();
        a.setUser(user); a.setProvider("GOOGLE"); a.setProviderUserId(providerUserId);
        authAccountRepository.save(a);
    }
    private Workspace saveWs(User owner, String name, String legalDomain, String country) {
        Workspace ws = new Workspace();
        ws.setName(name); ws.setSlug(name.toLowerCase().replace(' ', '-'));
        ws.setOwner(owner); ws.setLegalDomain(legalDomain); ws.setCountry(country);
        ws.setPlanCode("STARTER"); ws.setStatus("ACTIVE");
        return workspaceRepository.save(ws);
    }
    private void saveMember(User user, Workspace ws) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ws); m.setUser(user); m.setMemberRole("OWNER"); m.setPrimary(true);
        workspaceMemberRepository.save(m);
    }
    private CaseFile saveCf(User user, Workspace ws, String title, String domain) {
        CaseFile cf = new CaseFile();
        cf.setTitle(title); cf.setWorkspace(ws); cf.setCreatedBy(user);
        cf.setLegalDomain(domain); cf.setStatus("OPEN");
        return caseFileRepository.save(cf);
    }
    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
