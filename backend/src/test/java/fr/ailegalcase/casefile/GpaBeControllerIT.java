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
 * SF-223-04 : IT POST + GET + gate pays (BE) + gate domaine + validation
 * (lieu / lien génétique requis) + isolation workspace pour l'outil situation
 * contentieuse post-GPA BE.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class GpaBeControllerIT {

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
    private static final String SUFFIX = "/gpa-be-situation-contentieuse-analysis";

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        User uBe = save(u -> { u.setEmail("gpabe-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-gpabe-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEGPA " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCf = saveCf(uBe, wsBe, "CBEGPA " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-gpabe-be-" + ts, "gpabe-be-" + ts + "@ex.com");

        User uFr = save(u -> { u.setEmail("gpabe-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-gpabe-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRGPA " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        frCf = saveCf(uFr, wsFr, "CFRGPA " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-gpabe-fr-" + ts, "gpabe-fr-" + ts + "@ex.com");

        // Workspace tiers (isolation) : ne doit pas voir le dossier BE.
        User uOther = save(u -> { u.setEmail("gpabe-other-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOther, "g-gpabe-other-" + ts);
        Workspace wsOther = saveWs(uOther, "WSOTHERGPA " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uOther, wsOther);
        authOther = buildAuth("g-gpabe-other-" + ts, "gpabe-other-" + ts + "@ex.com");
    }

    private Map<String, Object> bodyReconnaissance() {
        Map<String, Object> m = new HashMap<>();
        m.put("gpaRealiseeEnBelgiqueOuEtranger", "BELGIQUE");
        m.put("lienGenetiqueParentIntentionnel", "PERE_INTENTIONNEL");
        m.put("acteNaissanceEtrangerEtabli", false);
        m.put("merePorteuseDesignee", true);
        m.put("consentementMerePorteuse", true);
        m.put("coupleIntentionnelMarieOuCohabitant", true);
        return m;
    }

    @Test
    void POST_be_reconnaissance_renvoie_200() throws Exception {
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyReconnaissance())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("FILIATION_PAR_RECONNAISSANCE"));
    }

    @Test
    void POST_be_aucun_lien_adoption_post_naissance_renvoie_200() throws Exception {
        Map<String, Object> body = bodyReconnaissance();
        body.put("lienGenetiqueParentIntentionnel", "AUCUN");
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("FILIATION_PAR_ADOPTION_POST_NAISSANCE"));
    }

    @Test
    void POST_be_etranger_acte_etranger_renvoie_200() throws Exception {
        Map<String, Object> body = bodyReconnaissance();
        body.put("gpaRealiseeEnBelgiqueOuEtranger", "ETRANGER");
        body.put("acteNaissanceEtrangerEtabli", true);
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE"));
    }

    @Test
    void POST_fr_returns400_gate_pays() throws Exception {
        mockMvc.perform(post(BASE + frCf.getId() + SUFFIX)
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyReconnaissance())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_lieu_absent_returns400() throws Exception {
        Map<String, Object> body = bodyReconnaissance();
        body.remove("gpaRealiseeEnBelgiqueOuEtranger");
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_lien_genetique_absent_returns400() throws Exception {
        Map<String, Object> body = bodyReconnaissance();
        body.remove("lienGenetiqueParentIntentionnel");
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyReconnaissance())))
                .andExpect(status().isOk());
        mockMvc.perform(get(BASE + beCf.getId() + SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("FILIATION_PAR_RECONNAISSANCE"));
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
                        .content(objectMapper.writeValueAsString(bodyReconnaissance())))
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
