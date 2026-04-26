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
class CommunauteUniverselleControllerIT {

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
        User uFr = save(new User(), u -> { u.setEmail("commun-univ-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-commun-univ-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSCUF " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFCUF " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-commun-univ-fr-" + ts, "commun-univ-fr-" + ts + "@ex.com");

        // BE workspace DROIT_FAMILLE (gate FRANCE → 400)
        User uBe = save(new User(), u -> { u.setEmail("commun-univ-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-commun-univ-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSCUB " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFCUB " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-commun-univ-be-" + ts, "commun-univ-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate domain → 400)
        User uDt = save(new User(), u -> { u.setEmail("commun-univ-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-commun-univ-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSCUDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFCUDT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-commun-univ-dt-" + ts, "commun-univ-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> validiteValidBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dispositifAnalyse", "VALIDITE_CONVENTION");
        body.put("contratNotarie", true);
        body.put("inscriptionEtatCivil", true);
        body.put("consentementLibreDesEpoux", true);
        body.put("respectReserveHereditaire", true);
        return body;
    }

    private Map<String, Object> liquidationCaiEnfantsNonCommunsBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dispositifAnalyse", "LIQUIDATION_DECES");
        body.put("contratNotarie", true);
        body.put("clauseAttributionIntegrale", true);
        body.put("enfantsNonCommuns", true);
        body.put("valeurCommunauteEur", 800000);
        return body;
    }

    @Test
    void POST_fr_validiteValide_returnsVALIDE() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validiteValidBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dispositifAnalyse").value("VALIDITE_CONVENTION"))
                .andExpect(jsonPath("$.verdictValidite").value("VALIDE"))
                .andExpect(jsonPath("$.actionRetranchementPossible").value(false))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("1526")))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("1527")));
    }

    @Test
    void POST_fr_validiteContratNonNotarie_returnsNUL() throws Exception {
        Map<String, Object> body = validiteValidBody();
        body.put("contratNotarie", false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictValidite").value("NUL"))
                .andExpect(jsonPath("$.partAttributionConjointPct").value(0));
    }

    @Test
    void POST_fr_liquidationCaiEnfantsNonCommuns_actionRetranchement() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(liquidationCaiEnfantsNonCommunsBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispositifAnalyse").value("LIQUIDATION_DECES"))
                .andExpect(jsonPath("$.actionRetranchementPossible").value(true))
                .andExpect(jsonPath("$.partAttributionConjointPct").value(100))
                .andExpect(jsonPath("$.valeurAttributionEur").value(800000.0))
                .andExpect(jsonPath("$.verdictValidite").value("CONTESTABLE"));
    }

    @Test
    void POST_fr_liquidationSansCai_partage50_50() throws Exception {
        Map<String, Object> body = liquidationCaiEnfantsNonCommunsBody();
        body.put("clauseAttributionIntegrale", false);
        body.put("enfantsNonCommuns", false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partAttributionConjointPct").value(50))
                .andExpect(jsonPath("$.valeurAttributionEur").value(400000.0))
                .andExpect(jsonPath("$.actionRetranchementPossible").value(false))
                .andExpect(jsonPath("$.verdictValidite").value("VALIDE"));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validiteValidBody())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validiteValidBody())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validiteValidBody())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_missingDispositif_returns400() throws Exception {
        Map<String, Object> body = validiteValidBody();
        body.remove("dispositifAnalyse");
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_liquidation_missingCai_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dispositifAnalyse", "LIQUIDATION_DECES");
        body.put("contratNotarie", true);
        body.put("enfantsNonCommuns", true);
        body.put("valeurCommunauteEur", 800000);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validiteValidBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictValidite").value("VALIDE"));

        Map<String, Object> next = validiteValidBody();
        next.put("contratNotarie", false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictValidite").value("NUL"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(liquidationCaiEnfantsNonCommunsBody())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dispositifAnalyse").value("LIQUIDATION_DECES"))
                .andExpect(jsonPath("$.actionRetranchementPossible").value(true));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/communaute-universelle-analysis")
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
