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

/**
 * SF-FA-19-03 : tests d'intégration changement de résidence
 * (art. 373-2 al. 3 Cciv).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ChangementResidenceControllerIT {

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
        User uFr = save(new User(), u -> { u.setEmail("cr-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-cr-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSCRFR " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFFR " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-cr-fr-" + ts, "cr-fr-" + ts + "@ex.com");

        // BE workspace DROIT_FAMILLE
        User uBe = save(new User(), u -> { u.setEmail("cr-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-cr-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSCRBE " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFBE " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-cr-be-" + ts, "cr-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL
        User uDt = save(new User(), u -> { u.setEmail("cr-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-cr-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSCRDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-cr-dt-" + ts, "cr-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate date,
                                     Integer distanceKm,
                                     String raison,
                                     Boolean consentement,
                                     Boolean informe,
                                     Integer delai,
                                     String mode,
                                     List<Integer> ages,
                                     Boolean scolarite,
                                     Boolean modifDvh) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateChangementPrevu", date != null ? date.toString() : null);
        m.put("distanceKm", distanceKm);
        m.put("raisonChangement", raison);
        m.put("consentementAutreParent", consentement);
        m.put("informePrealablement", informe);
        m.put("delaiInformationJours", delai);
        m.put("modeResidenceActuel", mode);
        m.put("ageEnfants", ages);
        m.put("scolariteImpactee", scolarite);
        m.put("modificationDvhDemandee", modifDvh);
        return m;
    }

    private Map<String, Object> bodyNominalElevee() {
        // info 30j + TRAVAIL + 50 km + consentement + pas scolarité = 100 → ELEVEE
        return body(LocalDate.of(2026, 9, 1),
                50, "TRAVAIL",
                true, true, 30,
                "EXCLUSIVE_DEMANDEUR",
                List.of(5, 7),
                false, false);
    }

    private Map<String, Object> bodyMoyenne250km() {
        // info 30j + TRAVAIL + 250 km + scolarité = 60 → MOYENNE
        return body(LocalDate.of(2026, 9, 1),
                250, "TRAVAIL",
                false, true, 30,
                "ALTERNEE",
                List.of(8, 12),
                true, true);
    }

    @Test
    void POST_fr_nominal_elevee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-residence")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreAcceptabilite").value(100))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"))
                .andExpect(jsonPath("$.obligationInformationRespectee").value(true))
                .andExpect(jsonPath("$.delaiPreavisLegalOk").value(true))
                .andExpect(jsonPath("$.expertisePsyEnfantRecommandee").value(false))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("373-2")));
    }

    @Test
    void POST_fr_moyenne_250km_score60() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-residence")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMoyenne250km())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreAcceptabilite").value(60))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("MOYENNE"));
    }

    @Test
    void POST_fr_faible_aucunCritere() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2026, 9, 1),
                500, "AUTRE",
                false, false, 0,
                "ALTERNEE", List.of(),
                true, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-residence")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreAcceptabilite").value(0))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"))
                .andExpect(jsonPath("$.obligationInformationRespectee").value(false));
    }

    @Test
    void POST_fr_raisonInvalide_returns400() throws Exception {
        Map<String, Object> b = body(LocalDate.of(2026, 9, 1),
                50, "INVALIDE",
                false, true, 30,
                "ALTERNEE", List.of(),
                false, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-residence")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/changement-residence")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/changement-residence")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authFr essaie d'accéder à dossier BE → 404 isolation
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/changement-residence")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-residence")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreAcceptabilite").value(100));

        // 2e appel : info absente, AUTRE, 500 km → score 0
        Map<String, Object> next = body(LocalDate.of(2026, 10, 1),
                500, "AUTRE",
                false, false, 0,
                "ALTERNEE", List.of(),
                true, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-residence")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreAcceptabilite").value(0))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/changement-residence")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMoyenne250km())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/changement-residence")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreAcceptabilite").value(60))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("MOYENNE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/changement-residence")
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
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
