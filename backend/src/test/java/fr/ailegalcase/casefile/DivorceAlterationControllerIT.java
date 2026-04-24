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
class DivorceAlterationControllerIT {

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

    /** 2 ans avant aujourd'hui — délai objectif ≥ 1 an satisfait. */
    private final LocalDate cessationIlYa2Ans = LocalDate.now().minusYears(2);

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR workspace DROIT_FAMILLE
        User uFr = save(new User(), u -> { u.setEmail("divalt-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-divalt-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRF " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFRF " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-divalt-fr-" + ts, "divalt-fr-" + ts + "@ex.com");

        // BE workspace DROIT_FAMILLE (gate country → rejet)
        User uBe = save(new User(), u -> { u.setEmail("divalt-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-divalt-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEF " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CBEF " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-divalt-be-" + ts, "divalt-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate legal_domain → rejet)
        User uDt = save(new User(), u -> { u.setEmail("divalt-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-divalt-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-divalt-dt-" + ts, "divalt-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate cessation,
                                     Boolean preuves,
                                     Boolean reconciliation,
                                     Integer dureeMariage,
                                     Number revenus1,
                                     Number revenus2,
                                     Boolean patrimoine,
                                     LocalDate assignation) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateCessationVieCommune", cessation != null ? cessation.toString() : null);
        m.put("preuvesSeparationDocumentaires", preuves);
        m.put("tentativesReconciliation", reconciliation);
        m.put("dureeMariageAnnees", dureeMariage);
        m.put("revenusAnnuelsEpoux1Eur", revenus1);
        m.put("revenusAnnuelsEpoux2Eur", revenus2);
        m.put("patrimoineCommunSignificatif", patrimoine);
        m.put("dateAssignation", assignation != null ? assignation.toString() : null);
        return m;
    }

    private Map<String, Object> bodyNominalElevee() {
        // 2 ans + preuves + pas reconciliation + mariage 10 ans → 100
        return body(cessationIlYa2Ans, true, false, 10, 60000, 30000, false, null);
    }

    @Test
    void POST_fr_nominal_elevee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/divorce-alteration")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.delaiObjectifOk").value(true))
                .andExpect(jsonPath("$.absencePreuveReconciliation").value(true))
                .andExpect(jsonPath("$.conditionsReunies").value(true))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("237")));
    }

    @Test
    void POST_fr_faible_separationCourteSansPreuves() throws Exception {
        Map<String, Object> body = body(LocalDate.now().minusMonths(6),
                false, true, 2, null, null, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/divorce-alteration")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(0))
                .andExpect(jsonPath("$.verdictProbabilite").value("FAIBLE"))
                .andExpect(jsonPath("$.criteresNonRemplis.length()")
                        .value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    void POST_fr_moyenne_delaiMaisPasPreuves() throws Exception {
        // 2 ans + pas preuves + pas reconciliation + mariage 3 ans → 40+0+20+0 = 60
        Map<String, Object> body = body(cessationIlYa2Ans, false, false, 3, null, null, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/divorce-alteration")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(60))
                .andExpect(jsonPath("$.verdictProbabilite").value("MOYENNE"));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/divorce-alteration")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/divorce-alteration")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authFr → dossier BE → isolation 404
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/divorce-alteration")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_futureCessation_returns400() throws Exception {
        Map<String, Object> body = body(LocalDate.now().plusDays(1),
                true, false, 10, null, null, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/divorce-alteration")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_missingDureeMariage_returns400() throws Exception {
        Map<String, Object> body = body(cessationIlYa2Ans,
                true, false, null, null, null, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/divorce-alteration")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/divorce-alteration")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(100));

        // Deuxième appel : dégrade, upsert (pas duplicate)
        Map<String, Object> next = body(cessationIlYa2Ans, false, true, 3, null, null, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/divorce-alteration")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                // 40 (delai) + 0 + 0 + 0 = 40
                .andExpect(jsonPath("$.scoreGlobal").value(40))
                .andExpect(jsonPath("$.verdictProbabilite").value("MOYENNE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/divorce-alteration")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/divorce-alteration")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabilite").value("ELEVEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/divorce-alteration")
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
