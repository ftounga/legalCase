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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class TestamentValiditeControllerIT {

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
    private OAuth2AuthenticationToken authDt;
    private CaseFile famFrCf;
    private CaseFile famBeCf;
    private CaseFile dtFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR workspace DROIT_FAMILLE
        User uFr = save(new User(), u -> { u.setEmail("test-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-test-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSTF " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFTF " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-test-fr-" + ts, "test-fr-" + ts + "@ex.com");

        // BE workspace DROIT_FAMILLE (gate FRANCE → 400)
        User uBe = save(new User(), u -> { u.setEmail("test-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-test-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSTB " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFTB " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-test-be-" + ts, "test-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate domain → 400)
        User uDt = save(new User(), u -> { u.setEmail("test-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-test-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSTDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFTDT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-test-dt-" + ts, "test-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> olographeValideBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("formeTestament", "TESTAMENT_OLOGRAPHE");
        body.put("dateRedaction", "2024-03-15");
        body.put("ageTestateurAnsRedaction", 72);
        body.put("saineDEsprit", true);
        body.put("majeurProtegeAvecAssistance", null);
        body.put("ecritureManuscritIntegrale", true);
        body.put("dateComplete", true);
        body.put("signatureTestateur", true);
        body.put("presenceNotaireEtTemoinsConforme", null);
        body.put("dicteEnPresence", null);
        body.put("lectureFinaleAuTestateur", null);
        body.put("signaturesCompletes", null);
        body.put("remiseSousPliCache", null);
        body.put("declarationDevant2Temoins", null);
        body.put("acteSuscriptionNotaire", null);
        body.put("respecteFormeWashington", null);
        body.put("vicesConsentementDol", false);
        body.put("erreurSubstantielle", false);
        body.put("testamentPosterieurContradictoire", false);
        body.put("dechirureVolontaireOriginal", false);
        body.put("legsExcedeQuotiteDisponible", false);
        return body;
    }

    @Test
    void POST_fr_olographeValide_returnsValide() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(olographeValideBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.formeTestament").value("TESTAMENT_OLOGRAPHE"))
                .andExpect(jsonPath("$.verdictValidite").value("VALIDE"))
                .andExpect(jsonPath("$.actionEnReductionPossible").value(false))
                .andExpect(jsonPath("$.delaiContestationAns").value(5))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("970")));
    }

    @Test
    void POST_fr_olographeNonManuscrit_returnsNul() throws Exception {
        Map<String, Object> body = olographeValideBody();
        body.put("ecritureManuscritIntegrale", false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictValidite").value("NUL"))
                .andExpect(jsonPath("$.vicesIdentifies[*].code")
                        .value(org.hamcrest.Matchers.hasItem("FORME_OLOGRAPHE_NON_MANUSCRITE")));
    }

    @Test
    void POST_fr_legsExcedeQuotite_actionReductionTrue() throws Exception {
        Map<String, Object> body = olographeValideBody();
        body.put("legsExcedeQuotiteDisponible", true);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictValidite").value("VALIDE"))
                .andExpect(jsonPath("$.actionEnReductionPossible").value(true));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(olographeValideBody())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(olographeValideBody())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // Auth FR tente d'accéder au case file BE
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(olographeValideBody())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_formeManquante_returns400() throws Exception {
        Map<String, Object> body = olographeValideBody();
        body.put("formeTestament", null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(olographeValideBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictValidite").value("VALIDE"));

        Map<String, Object> next = olographeValideBody();
        next.put("ecritureManuscritIntegrale", false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictValidite").value("NUL"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(olographeValideBody())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.formeTestament").value("TESTAMENT_OLOGRAPHE"))
                .andExpect(jsonPath("$.verdictValidite").value("VALIDE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/testament-validite-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ---- helpers ----

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
