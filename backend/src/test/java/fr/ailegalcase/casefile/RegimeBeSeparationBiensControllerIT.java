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
 * SF-223-06 : IT POST + GET + gate pays (BE) + gate domaine + validation
 * (variante requise, clause manquante pour participation, date future,
 * patrimoine négatif) + isolation workspace pour l'outil régime de séparation
 * de biens BE.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class RegimeBeSeparationBiensControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authOther;
    private CaseFile beCf;
    private CaseFile frCf;

    private static final String BASE = "/api/v1/case-files/";
    private static final String SUFFIX = "/regime-be-separation-biens-analysis";

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        User uBe = save(u -> { u.setEmail("rsb-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rsb-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBERSB " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCf = saveCf(uBe, wsBe, "CBERSB " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-rsb-be-" + ts, "rsb-be-" + ts + "@ex.com");

        User uFr = save(u -> { u.setEmail("rsb-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rsb-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRRSB " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        frCf = saveCf(uFr, wsFr, "CFRRSB " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-rsb-fr-" + ts, "rsb-fr-" + ts + "@ex.com");

        // Workspace tiers (isolation) : ne doit pas voir le dossier BE.
        User uOther = save(u -> { u.setEmail("rsb-other-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOther, "g-rsb-other-" + ts);
        Workspace wsOther = saveWs(uOther, "WSOTHERRSB " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uOther, wsOther);
        authOther = buildAuth("g-rsb-other-" + ts, "rsb-other-" + ts + "@ex.com");
    }

    private Map<String, Object> bodyPure() {
        Map<String, Object> m = new HashMap<>();
        m.put("varianteRegime", "SEPARATION_PURE");
        m.put("contratMariageNotarie", true);
        m.put("disproportionPatrimonialeAllegee", false);
        m.put("dateContrat", "2020-01-01");
        return m;
    }

    @Test
    void POST_be_separation_pure_renvoie_200() throws Exception {
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyPure())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("SEPARATION_PURE_QUALIFIEE"));
    }

    @Test
    void POST_be_participation_acquets_renvoie_200() throws Exception {
        Map<String, Object> body = bodyPure();
        body.put("varianteRegime", "SEPARATION_AVEC_PARTICIPATION_ACQUETS");
        body.put("clauseParticipationPrevue", true);
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PARTICIPATION_ACQUETS_QUALIFIEE"));
    }

    @Test
    void POST_fr_returns400_gate_pays() throws Exception {
        mockMvc.perform(post(BASE + frCf.getId() + SUFFIX)
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyPure())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_variante_absente_returns400() throws Exception {
        Map<String, Object> body = bodyPure();
        body.remove("varianteRegime");
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_clause_manquante_pour_participation_returns400() throws Exception {
        Map<String, Object> body = bodyPure();
        body.put("varianteRegime", "SEPARATION_AVEC_PARTICIPATION_ACQUETS");
        // clauseParticipationPrevue absente
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_date_future_returns400() throws Exception {
        Map<String, Object> body = bodyPure();
        body.put("dateContrat", "2999-01-01");
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_patrimoine_negatif_returns400() throws Exception {
        Map<String, Object> body = bodyPure();
        body.put("patrimoinePropreEpoux1Eur", -1);
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyPure())))
                .andExpect(status().isOk());
        mockMvc.perform(get(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SEPARATION_PURE_QUALIFIEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_autreWorkspace_returns404_isolation() throws Exception {
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyPure())))
                .andExpect(status().isOk());
        mockMvc.perform(get(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authOther)))
                .andExpect(status().isNotFound());
    }

    // helpers
    private User save(java.util.function.Consumer<User> init) {
        User u = new User();
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
