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
class ReferesAdminControllerIT {

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
    private CaseFile immFrCf;
    private CaseFile immBeCf;
    private CaseFile dtFrCf;

    private final LocalDate notifRecente = LocalDate.now().minusDays(10);

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR workspace DROIT_IMMIGRATION
        User uFr = save(new User(), u -> { u.setEmail("ref-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-ref-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRR " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRR " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-ref-fr-" + ts, "ref-fr-" + ts + "@ex.com");

        // BE workspace DROIT_IMMIGRATION (gate country FRANCE → rejet)
        User uBe = save(new User(), u -> { u.setEmail("ref-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-ref-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBER " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBER " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-ref-be-" + ts, "ref-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate legal_domain → rejet)
        User uDt = save(new User(), u -> { u.setEmail("ref-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-ref-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRTr " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRTr " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-ref-dt-" + ts, "ref-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String type, String decision, LocalDate notif,
                                     boolean urgence, boolean atteinte, boolean doutes,
                                     List<String> preuves, boolean dejaPrived) {
        Map<String, Object> m = new HashMap<>();
        m.put("typeRefere", type);
        m.put("decisionContestee", decision);
        m.put("dateNotificationDecision", notif.toString());
        m.put("urgenceCaracterisee", urgence);
        m.put("atteinteLiberteFondamentale", atteinte);
        m.put("doutesSerieuxLegalite", doutes);
        m.put("preuvesUrgence", preuves);
        m.put("demandeurDejaPrived", dejaPrived);
        return m;
    }

    @Test
    void POST_fr_suspension_nominal_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/referes-admin")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("SUSPENSION", "OQTF", notifRecente,
                                        true, false, true, List.of("MENACE_VIE_PRIVEE"), false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRefere").value("SUSPENSION"))
                .andExpect(jsonPath("$.decisionContestee").value("OQTF"))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreSuccessProbabiliteSuspension").exists())
                .andExpect(jsonPath("$.scoreSuccessProbabiliteLiberte").exists())
                .andExpect(jsonPath("$.verdictRecommandation").exists())
                .andExpect(jsonPath("$.delaiJugeTaJoursL521_1").value(30))
                .andExpect(jsonPath("$.delaiJugeTaHeuresL521_2").value(48))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("L.521-1")));
    }

    @Test
    void POST_fr_lesDeux_returnsLesDeuxCumules() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/referes-admin")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("LES_DEUX", "OQTF", notifRecente,
                                        true, true, true, List.of(), false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRefere").value("LES_DEUX"))
                .andExpect(jsonPath("$.verdictRecommandation").value("LES_DEUX_CUMULES"))
                .andExpect(jsonPath("$.conditionsCumulativesL521_1Ok").value(true))
                .andExpect(jsonPath("$.conditionsCumulativesL521_2Ok").value(true));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/referes-admin")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("SUSPENSION", "OQTF", notifRecente,
                                        true, false, true, List.of(), false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/referes-admin")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("SUSPENSION", "OQTF", notifRecente,
                                        true, false, true, List.of(), false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/referes-admin")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("SUSPENSION", "OQTF", notifRecente,
                                        true, false, true, List.of(), false))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_invalidTypeRefere_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/referes-admin")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("BIDON", "OQTF", notifRecente,
                                        true, false, true, List.of(), false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/referes-admin")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("SUSPENSION", "OQTF", notifRecente,
                                        true, false, true, List.of(), false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRefere").value("SUSPENSION"));

        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/referes-admin")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("LIBERTE", "IRTF", notifRecente,
                                        true, true, false, List.of(), true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRefere").value("LIBERTE"))
                .andExpect(jsonPath("$.decisionContestee").value("IRTF"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/referes-admin")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("SUSPENSION", "RETRAIT_TITRE", notifRecente,
                                        true, false, true, List.of("TRANSFERT_IMMINENT"), false))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/referes-admin")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRefere").value("SUSPENSION"))
                .andExpect(jsonPath("$.decisionContestee").value("RETRAIT_TITRE"))
                .andExpect(jsonPath("$.country").value("FRANCE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/referes-admin")
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
