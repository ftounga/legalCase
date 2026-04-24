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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
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
class Belgian40terControllerIT {

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

        // BE workspace DROIT_IMMIGRATION (cible)
        User uBe = save(new User(), u -> { u.setEmail("40ter-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-40ter-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-40ter-be-" + ts, "40ter-be-" + ts + "@ex.com");

        // FR workspace DROIT_IMMIGRATION (gate country BELGIQUE → rejet)
        User uFr = save(new User(), u -> { u.setEmail("40ter-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-40ter-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-40ter-fr-" + ts, "40ter-fr-" + ts + "@ex.com");

        // BE workspace DROIT_DU_TRAVAIL (gate legal_domain → rejet)
        User uDt = save(new User(), u -> { u.setEmail("40ter-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-40ter-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBET " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBET " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-40ter-dt-" + ts, "40ter-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String lien,
                                     Boolean regroupantBelge,
                                     BigDecimal revenus,
                                     BigDecimal seuil,
                                     Boolean assurance,
                                     Boolean logement,
                                     Boolean menace,
                                     LocalDate depot) {
        Map<String, Object> m = new HashMap<>();
        m.put("lienFamilial", lien);
        m.put("regroupantBelge", regroupantBelge);
        m.put("revenusMensuelsNetsEur", revenus);
        m.put("seuil120PctRisEur", seuil);
        m.put("assuranceMaladie", assurance);
        m.put("logementSuffisant", logement);
        m.put("menaceOrdrePublic", menace);
        m.put("dateDepotDemande", depot != null ? depot.toString() : null);
        return m;
    }

    private Map<String, Object> bodyNominalElevee() {
        return body("CONJOINT", true, new BigDecimal("2500"),
                new BigDecimal("1740"), true, true, false, null);
    }

    @Test
    void POST_be_nominal_elevee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.lienValide").value(true))
                .andExpect(jsonPath("$.regroupantBelgeOk").value(true))
                .andExpect(jsonPath("$.revenusSuffisantsOk").value(true))
                .andExpect(jsonPath("$.assuranceOk").value(true))
                .andExpect(jsonPath("$.logementOk").value(true))
                .andExpect(jsonPath("$.pasMenace").value(true))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("40ter")));
    }

    @Test
    void POST_be_revenusInsuffisants_elevee_degradee() throws Exception {
        // revenus < seuil mais tout le reste OK → 5 × 18 = 90 ELEVEE
        Map<String, Object> body = body("CONJOINT", true,
                new BigDecimal("1600"), new BigDecimal("1740"),
                true, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenusSuffisantsOk").value(false))
                .andExpect(jsonPath("$.scoreGlobal").value(90))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"));
    }

    @Test
    void POST_be_faible_plusieursConditionsKo() throws Exception {
        Map<String, Object> body = body("CONJOINT", false,
                new BigDecimal("500"), new BigDecimal("1740"),
                false, false, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(18)) // seul lien OK
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"))
                .andExpect(jsonPath("$.criteresNonRemplis.length()")
                        .value(org.hamcrest.Matchers.greaterThanOrEqualTo(5)));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/belgian-40ter")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe → dossier FR → isolation 404
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_lienInvalide_returns400() throws Exception {
        Map<String, Object> body = body("CONCUBIN_SIMPLE", true,
                new BigDecimal("1800"), new BigDecimal("1740"),
                true, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_missingLienFamilial_returns400() throws Exception {
        Map<String, Object> body = body(null, true,
                new BigDecimal("1800"), new BigDecimal("1740"),
                true, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_missingRevenus_returns400() throws Exception {
        Map<String, Object> body = body("CONJOINT", true,
                null, new BigDecimal("1740"),
                true, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(100));

        Map<String, Object> next = body("CONJOINT", false,
                new BigDecimal("500"), new BigDecimal("1740"),
                false, false, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(18))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40ter")
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
