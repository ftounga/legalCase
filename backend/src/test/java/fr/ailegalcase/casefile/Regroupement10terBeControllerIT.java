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

/** SF-215-03 — IT regroupement 10ter BE. */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class Regroupement10terBeControllerIT {

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
    private OAuth2AuthenticationToken authDt;
    private CaseFile immBeCf;
    private CaseFile immFrCf;
    private CaseFile dtBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uBe = save(new User(), u -> { u.setEmail("regr10ter-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-regr10ter-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI10TER " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI10TER " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-regr10ter-be-" + ts, "regr10ter-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("regr10ter-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-regr10ter-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI10TER " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI10TER " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-regr10ter-fr-" + ts, "regr10ter-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("regr10ter-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-regr10ter-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBET10TER " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBET10TER " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-regr10ter-dt-" + ts, "regr10ter-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String lienFamilial, String typeCarte, int revenus, int duree,
                                     boolean logementConforme, boolean assuranceMaladie,
                                     boolean menaceOrdrePublic) {
        Map<String, Object> m = new HashMap<>();
        m.put("lienFamilial", lienFamilial);
        m.put("typeCarteRegroupant", typeCarte);
        m.put("revenusMensuelsNetsRegroupant", revenus);
        m.put("dureeSejour", duree);
        m.put("logementConforme", logementConforme);
        m.put("assuranceMaladie", assuranceMaladie);
        m.put("menaceOrdrePublic", menaceOrdrePublic);
        return m;
    }

    @Test
    void POST_be_nominal_returns200_eligible() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", "CARTE_B", 1_600, 24, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lienFamilial").value("CONJOINT"))
                .andExpect(jsonPath("$.typeCarteRegroupant").value("CARTE_B"))
                .andExpect(jsonPath("$.scoreEligibilite").value(100))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.differentielRevenus").value(100))
                .andExpect(jsonPath("$.seuilRessources").value(1500))
                .andExpect(jsonPath("$.criteresNonRemplis").isArray())
                .andExpect(jsonPath("$.criteresNonRemplis").isEmpty())
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("10 et 10ter")));
    }

    @Test
    void POST_be_revenus1499_returnsSousReserve_differentielNegatif() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PARTENAIRE_ENREGISTRE", "CARTE_C", 1_499, 24, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SOUS_RESERVE"))
                .andExpect(jsonPath("$.scoreEligibilite").value(60))
                .andExpect(jsonPath("$.differentielRevenus").value(-1));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", "CARTE_B", 1_600, 24, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", "CARTE_B", 1_600, 24, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe tente d'accéder au dossier du workspace FR → 404 (isolation)
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", "CARTE_B", 1_600, 24, true, true, false))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_invalidLienFamilial_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("LIEN_INCONNU", "CARTE_B", 1_600, 24, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_revenusOutOfBound_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", "CARTE_B", 100_001, 24, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", "CARTE_B", 1_600, 24, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"));

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("ASCENDANT_CHARGE", "CARTE_C", 0, 0, false, false, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE"))
                .andExpect(jsonPath("$.scoreEligibilite").value(0));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("ENFANT_MOINS_21", "CARTE_C", 2_000, 30, true, true, false))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lienFamilial").value("ENFANT_MOINS_21"))
                .andExpect(jsonPath("$.typeCarteRegroupant").value("CARTE_C"))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/regroupement-10ter-be-analysis")
                        .with(authentication(authBe)))
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
