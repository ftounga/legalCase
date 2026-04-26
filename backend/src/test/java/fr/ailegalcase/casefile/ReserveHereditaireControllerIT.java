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
import java.time.LocalDate;
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
class ReserveHereditaireControllerIT {

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

        // FR DROIT_FAMILLE
        User uFr = save(new User(), u -> { u.setEmail("rh-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rh-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSRHF " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFRHF " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-rh-fr-" + ts, "rh-fr-" + ts + "@ex.com");

        // BE DROIT_FAMILLE
        User uBe = save(new User(), u -> { u.setEmail("rh-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rh-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSRHB " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFRHB " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-rh-be-" + ts, "rh-be-" + ts + "@ex.com");

        // FR DROIT_DU_TRAVAIL (mauvais domaine)
        User uDt = save(new User(), u -> { u.setEmail("rh-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-rh-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSRHDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRHDT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-rh-dt-" + ts, "rh-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> nominalBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nombreEnfants", 1);
        body.put("conjointSurvivant", false);
        body.put("montantSuccession", 200000);
        body.put("montantLibsTotal", 150000);
        body.put("dateOuvertureSuccession", LocalDate.now().minusMonths(6).toString());
        body.put("qualiteDuDemandeur", "HERITIER_RESERVATAIRE_DESCENDANT");
        return body;
    }

    @Test
    void POST_fr_nominal_excedent50k_returnsRecevable() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.quotiteDisponiblePct").value(50.0))
                .andExpect(jsonPath("$.reservePct").value(50.0))
                .andExpect(jsonPath("$.excedentReductibleEur").value(50000.00))
                .andExpect(jsonPath("$.actionReductionRecevable").value(true))
                .andExpect(jsonPath("$.verdictRecevabilite").value("RECEVABLE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("913")));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("RECEVABLE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_nombreEnfantsNegatif_returns400() throws Exception {
        Map<String, Object> body = nominalBody();
        body.put("nombreEnfants", -1);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionReductionRecevable").value(true));

        Map<String, Object> next = nominalBody();
        next.put("montantLibsTotal", 50000); // libs < QD
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionReductionRecevable").value(false))
                .andExpect(jsonPath("$.verdictRecevabilite").value("NON_RECEVABLE_PAS_EXCEDENT"));
    }

    @Test
    void POST_prescriptionEcoulee_returns200_actionNonRecevable() throws Exception {
        Map<String, Object> body = nominalBody();
        body.put("dateOuvertureSuccession", LocalDate.now().minusYears(6).toString());
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/reserve-heriditaire-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionReductionRecevable").value(false))
                .andExpect(jsonPath("$.verdictRecevabilite").value("NON_RECEVABLE_PRESCRIPTION"));
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
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
