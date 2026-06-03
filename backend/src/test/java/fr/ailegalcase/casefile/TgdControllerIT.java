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

/**
 * SF-222-02 : IT POST + GET + gate BE + isolation workspace pour l'outil TGD
 * (téléphone grave danger — éligibilité — F-FA-TGD).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class TgdControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authOther;
    private CaseFile frCf;
    private CaseFile beCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        User uFr = save(new User(), u -> { u.setEmail("tgd-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-tgd-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRTGD " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        frCf = saveCf(uFr, wsFr, "CFRTGD " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-tgd-fr-" + ts, "tgd-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("tgd-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-tgd-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBETGD " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCf = saveCf(uBe, wsBe, "CBETGD " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-tgd-be-" + ts, "tgd-be-" + ts + "@ex.com");

        // Workspace tiers (isolation) : ne doit pas voir le dossier FR.
        User uOther = save(new User(), u -> { u.setEmail("tgd-other-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOther, "g-tgd-other-" + ts);
        Workspace wsOther = saveWs(uOther, "WSOTHERTGD " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uOther, wsOther);
        authOther = buildAuth("g-tgd-other-" + ts, "tgd-other-" + ts + "@ex.com");
    }

    private Map<String, Object> bodyEligible() {
        Map<String, Object> m = new HashMap<>();
        m.put("dangerGrave", true);
        m.put("violencesAvereesOuVraisemblables", true);
        m.put("interdictionContactProcedure", true);
        m.put("nonCohabitation", true);
        m.put("consentementVictime", true);
        return m;
    }

    @Test
    void POST_fr_tous_criteres_renvoie_200_eligible() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + frCf.getId() + "/tgd-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_TGD"));
    }

    @Test
    void POST_fr_interdiction_manquante_renvoie_sous_reserve() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("interdictionContactProcedure", false);
        mockMvc.perform(post("/api/v1/case-files/" + frCf.getId() + "/tgd-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_SOUS_RESERVE"))
                .andExpect(jsonPath("$.criteresManquants").isNotEmpty());
    }

    @Test
    void POST_be_returns400_gate_pays() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + beCf.getId() + "/tgd-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + frCf.getId() + "/tgd-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + frCf.getId() + "/tgd-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_TGD"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + frCf.getId() + "/tgd-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_autreWorkspace_returns404_isolation() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + frCf.getId() + "/tgd-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk());

        // L'utilisateur d'un autre workspace ne doit pas accéder au dossier FR.
        mockMvc.perform(get("/api/v1/case-files/" + frCf.getId() + "/tgd-analysis")
                        .with(authentication(authOther)))
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
